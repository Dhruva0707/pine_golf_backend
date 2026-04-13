import { useState, useEffect } from 'react';
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

export const PlayersView = ({ isAdmin, currentUserName }: PlayersViewProps) => {
    // 1. STATE MANAGEMENT
    const [players, setPlayers] = useState<Player[]>([]);
    const [teams, setTeams] = useState<any[]>([]);
    const [search, setSearch] = useState('');
    const [loading, setLoading] = useState(true);

    // Modal States
    const [isAddModalOpen, setIsAddModalOpen] = useState(false);
    const [isPassModalOpen, setIsPassModalOpen] = useState(false);
    const [isNameModalOpen, setIsNameModalOpen] = useState(false);
    const [selectedPlayerName, setSelectedPlayerName] = useState('');

    // 2. DATA FETCHING
    const fetchData = async () => {
        setLoading(true);
        try {
            const [playerRes, teamRes] = await Promise.all([
                api.get('/players'),
                api.get('/teams')
            ]);
            setPlayers(Array.isArray(playerRes.data) ? playerRes.data : []);
            setTeams(Array.isArray(teamRes.data) ? teamRes.data : []);
        } catch (err) {
            console.error("Error fetching data:", err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchData();
    }, []);

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
                        <th className="px-8 py-5 text-xs font-black uppercase tracking-widest text-latte-subtext">Team Status</th>
                        <th className="px-8 py-5 text-xs font-black uppercase tracking-widest text-latte-subtext text-center">Actions</th>
                    </tr>
                    </thead>
                    <tbody className="divide-y divide-latte-crust">
                    {filteredPlayers.map(p => (
                        <tr key={p.name} className="hover:bg-latte-base/30 transition-colors">
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