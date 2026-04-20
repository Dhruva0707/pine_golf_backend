import { useState, useEffect, useRef } from 'react';
import { Trash2, User, Plus, Search, Lock, Edit2, UserRoundPen } from 'lucide-react';
import api from '../../api/client';
import { AddPlayerModal } from './AddPlayerModal.tsx';
import { ChangePasswordModal } from './ChangePasswordModal.tsx';
import { ChangePlayerNameModal } from './ChangePlayerNameModal.tsx';

interface Player {
    id: number;
    name: string;
    handicap: number;
    team?: string;
}

interface PlayersViewProps {
    isAdmin: boolean;
    currentUserName: string | null;
}

// --- New types for courses and course-handicap responses ---
interface Course { id: number; name: string; }
interface CourseHandicapResponse { handicap: number; }

export const PlayersView = ({ isAdmin, currentUserName }: PlayersViewProps) => {
    // 1. STATE MANAGEMENT
    const [players, setPlayers] = useState<Player[]>([]);
    const [teams, setTeams] = useState<any[]>([]);
    const [search, setSearch] = useState('');
    const [loading, setLoading] = useState(true);

    // New state: list of courses and per-player selected course
    const [courses, setCourses] = useState<Course[]>([]);
    const [selectedCourseByPlayer, setSelectedCourseByPlayer] = useState<Record<number, number | "">>({});

    // Cache for fetched course handicaps keyed by `${playerId}_${courseId}`
    const courseHandicapCacheRef = useRef<Map<string, number>>(new Map());
    const [loadingCourseKeys, setLoadingCourseKeys] = useState<Record<string, boolean>>({});
    const [, forceRerender] = useState(0); // used to re-render after mutating ref

    // Modal States
    const [isAddModalOpen, setIsAddModalOpen] = useState(false);
    const [isPassModalOpen, setIsPassModalOpen] = useState(false);
    const [isNameModalOpen, setIsNameModalOpen] = useState(false);
    const [selectedPlayerName, setSelectedPlayerName] = useState('');

    // 2. DATA FETCHING
    const fetchData = async () => {
        setLoading(true);
        try {
            const [playerRes, teamRes, courseRes] = await Promise.all([
                api.get('/players'),
                api.get('/teams'),
                api.get('/courses')
            ]);
            setPlayers(Array.isArray(playerRes.data) ? playerRes.data : []);
            setTeams(Array.isArray(teamRes.data) ? teamRes.data : []);
            setCourses(Array.isArray(courseRes.data) ? courseRes.data : []);
        } catch (err) {
            console.error("Error fetching data:", err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchData();
    }, []);

    // --- New: select a course for a player and fetch course-handicap if needed ---
    const onSelectCourse = async (playerId: number, courseId: number) => {
        setSelectedCourseByPlayer(prev => ({ ...prev, [playerId]: courseId }));
        const key = `${playerId}_${courseId}`;

        if (courseHandicapCacheRef.current.has(key)) {
            forceRerender(n => n + 1);
            return;
        }

        // mark loading for this key
        setLoadingCourseKeys(prev => ({ ...prev, [key]: true }));
        try {
            const res = await api.get<CourseHandicapResponse>(`/players/${playerId}/${courseId}/handicap`);
            // Controller returns CourseHandicap entity; we read `handicap` from it
            const handicap = res.data && (res.data as any).handicap !== undefined ? (res.data as any).handicap : undefined;
            if (handicap !== undefined) {
                courseHandicapCacheRef.current.set(key, handicap);
            }
        } catch (err) {
            console.error('Failed to load player course handicap', err);
        } finally {
            setLoadingCourseKeys(prev => {
                const next = { ...prev } as Record<string, boolean>;
                delete next[key];
                return next;
            });
            forceRerender(n => n + 1);
        }
    };

    // --- New: admin can update course handicap for a player ---
    const handleUpdateCourseHandicap = async (playerId: number, courseId: number) => {
        const input = window.prompt('New course handicap (e.g. 12.3):');
        if (input === null) return; // cancelled
        const parsed = parseFloat(input);
        if (isNaN(parsed)) {
            alert('Enter a valid number');
            return;
        }

        try {
            // Controller expects a raw double in the request body
            await api.post(
                `/players/${playerId}/${courseId}/handicap`,
                parsed,

                {
                    headers: {
                        'Content-Type': 'application/json',
                    },
                }
            );
            const key = `${playerId}_${courseId}`;
            courseHandicapCacheRef.current.set(key, parsed);
            forceRerender(n => n + 1);
        } catch (err) {
            console.error('Failed to update course handicap', err);
            alert('Failed to update handicap.');
        }
    };

    // 3. ACTION HANDLERS
    const handleUpdateHandicap = async (name: string, current: number) => {
        const newVal = window.prompt(`Update handicap for ${name}:`, current.toString());

        // Check if user cancelled or entered nothing
        if (newVal === null || newVal.trim() === "") return;

        const parsedHandicap = parseFloat(newVal);
        if (isNaN(parsedHandicap)) {
            alert("Please enter a valid number (e.g., 12.5)");
            return;
        }

        try {
            // Pointing to /players/{name} (Admin Only)
            // Sending an object { handicap: ... } to match your UpdatePlayerRequest DTO
            await api.put(`/players/${encodeURIComponent(name)}`, {
                handicap: parsedHandicap
            });
            fetchData();
        } catch (err) {
            alert("Failed to update handicap. Make sure you have Admin permissions.");
        }
    };

    const handleUpdateTeam = async (playerName: string, teamName: string) => {
        try {
            // Pointing to the main profile update endpoint
            await api.put(`/players/${encodeURIComponent(playerName)}`, {
                teamName: teamName
            });
            fetchData();
        } catch (err) {
            alert("Update failed.");
        }
    };

    const handleDelete = async (player: Player) => {
        if (window.confirm(`Permanently delete ${player.name}?`)) {
            try {
                await api.delete(`/players/${encodeURIComponent(player.name)}`);
                fetchData();
            } catch (err) {
                alert("Could not delete player. They may have active scores.");
            }
        }
    };

    // 4. SEARCH FILTERING
    const filteredPlayers = players.filter(p =>
        p.name?.toLowerCase().includes(search.toLowerCase())
    );

    return (
        <div className="space-y-6 animate-in fade-in duration-500">
            {/* Header */}
            <div className="flex flex-wrap justify-between items-end gap-3">
                <div>
                    <h2 className="text-3xl font-black text-latte-text">Roster</h2>
                    <p className="text-latte-subtext font-medium">
                        {loading ? 'Refreshing...' : `${filteredPlayers.length} golfers found`}
                    </p>
                </div>
                {isAdmin && (
                    <button
                        onClick={() => setIsAddModalOpen(true)}
                        className="bg-latte-green text-white px-6 py-3 rounded-2xl font-bold flex items-center gap-2 hover:brightness-110 shadow-lg"
                    >
                        <Plus size={20} /> Add Player
                    </button>
                )}
            </div>

            {/* Search */}
            <div className="relative">
                <Search className="absolute left-4 top-4 text-latte-subtext" size={20} />
                <input
                    className="w-full p-4 pl-12 rounded-2xl border border-latte-crust outline-none focus:ring-2 focus:ring-latte-mauve bg-white shadow-sm"
                    placeholder="Search golfers by name..."
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                />
            </div>

            {/* Table */}
            <div className="bg-white rounded-3xl border border-latte-crust shadow-sm overflow-x-auto no-scrollbar">
                <table className="w-full text-left border-collapse min-w-[640px]">
                    <thead className="bg-latte-mantle/50 border-b border-latte-crust">
                    <tr>
                        <th className="px-8 py-5 text-xs font-black uppercase tracking-widest text-latte-subtext">Golfer</th>
                        <th className="px-8 py-5 text-xs font-black uppercase tracking-widest text-latte-subtext">Handicap</th>
                        <th className="px-8 py-5 text-xs font-black uppercase tracking-widest text-latte-subtext">Course Handicap</th>
                        <th className="px-8 py-5 text-xs font-black uppercase tracking-widest text-latte-subtext">Team Status</th>
                        <th className="px-8 py-5 text-xs font-black uppercase tracking-widest text-latte-subtext text-center">Actions</th>
                    </tr>
                    </thead>
                    <tbody className="divide-y divide-latte-crust">
                    {filteredPlayers.map(p => (
                        <tr key={p.id} className="hover:bg-latte-base/30 transition-colors">
                            <td className="px-8 py-5 font-bold text-latte-text">
                                <div className="flex items-center gap-3">
                                    <User size={16} className="text-latte-subtext" />
                                    <span>{p.name}</span>
                                    {isAdmin && (
                                        <button
                                            onClick={() => { setSelectedPlayerName(p.name); setIsNameModalOpen(true); }}
                                            className="text-latte-subtext hover:text-latte-blue transition-colors"
                                            title="Change Name"
                                        >
                                            <UserRoundPen size={14} />
                                        </button>
                                    )}
                                </div>
                            </td>

                            <td className="px-8 py-5">
                                <div className="flex items-center gap-2">
                                    <span className="bg-latte-base px-3 py-1 rounded-lg font-mono font-bold text-latte-blue">
                                        {p.handicap}
                                    </span>
                                    {isAdmin && (
                                        <button
                                            onClick={() => handleUpdateHandicap(p.name, p.handicap)}
                                            className="text-latte-subtext hover:text-latte-blue transition-colors"
                                        >
                                            <Edit2 size={14} />
                                        </button>
                                    )}
                                </div>
                            </td>

                            {/* New: Course Handicap dropdown & display */}
                            <td className="px-8 py-5">
                                <div className="flex items-center gap-3">
                                    <select
                                        className="bg-transparent border-b border-latte-crust text-sm font-medium outline-none focus:border-latte-green cursor-pointer"
                                        value={selectedCourseByPlayer[p.id] ?? ""}
                                        onChange={(e) => onSelectCourse(p.id, Number(e.target.value))}
                                    >
                                        <option value="">Select Course</option>
                                        {courses.map(c => (
                                            <option key={c.id} value={c.id}>{c.name}</option>
                                        ))}
                                    </select>

                                    {/* show handicap or loading */}
                                    {(() => {
                                        const sel = selectedCourseByPlayer[p.id];
                                        if (!sel) return <span className="text-latte-subtext">—</span>;
                                        const key = `${p.id}_${sel}`;
                                        if (loadingCourseKeys[key]) return <span className="text-latte-subtext">Loading...</span>;
                                        const cached = courseHandicapCacheRef.current.get(key);
                                        return cached !== undefined ? (
                                            <div className="flex items-center gap-1">
                                                <span className="bg-latte-base px-3 py-1 rounded-lg font-mono font-bold text-latte-blue">{cached}</span>
                                                {isAdmin && (
                                                    <button
                                                        onClick={() => handleUpdateCourseHandicap(p.id, Number(sel))}
                                                        className="text-latte-subtext hover:text-latte-blue transition-colors"
                                                        title="Edit Course Handicap"
                                                    >
                                                        <Edit2 size={14} />
                                                    </button>
                                                )}
                                            </div>
                                        ) : (
                                            <span className="text-latte-subtext">—</span>
                                        );
                                    })()}
                                </div>
                            </td>

                            <td className="px-8 py-5">
                                {isAdmin ? (
                                    <select
                                        className="bg-transparent border-b border-latte-crust text-sm font-medium outline-none focus:border-latte-green cursor-pointer"
                                        value={p.team || ""}
                                        onChange={(e) => handleUpdateTeam(p.name, e.target.value)}
                                    >
                                        <option value="">Independent</option>
                                        {teams.map(t => (
                                            <option key={t.id} value={t.name}>{t.name}</option>
                                        ))}
                                    </select>
                                ) : (
                                    <span className="text-latte-subtext font-medium">{p.team || 'Independent'}</span>
                                )}
                            </td>

                            <td className="px-8 py-5">
                                <div className="flex items-center justify-center gap-3">
                                    {(isAdmin || currentUserName === p.name) && (
                                        <button
                                            onClick={() => { setSelectedPlayerName(p.name); setIsPassModalOpen(true); }}
                                            className="p-2 text-latte-mauve hover:bg-latte-mauve/10 rounded-xl transition-colors"
                                            title="Change Password"
                                        >
                                            <Lock size={18} />
                                        </button>
                                    )}

                                    {isAdmin && (
                                        <button
                                            onClick={() => handleDelete(p)}
                                            className="p-2 text-latte-red hover:bg-latte-red/10 rounded-xl transition-colors"
                                            title="Delete Player"
                                        >
                                            <Trash2 size={18}/>
                                        </button>
                                    )}
                                </div>
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>

            {/* Modals */}
            <AddPlayerModal
                isOpen={isAddModalOpen}
                onClose={() => setIsAddModalOpen(false)}
                onSuccess={fetchData}
            />

            <ChangePasswordModal
                isOpen={isPassModalOpen}
                onClose={() => setIsPassModalOpen(false)}
                playerName={selectedPlayerName}
            />

            <ChangePlayerNameModal
                isOpen={isNameModalOpen}
                onClose={() => setIsNameModalOpen(false)}
                currentName={selectedPlayerName}
                onSuccess={fetchData}
            />
        </div>
    );
};

