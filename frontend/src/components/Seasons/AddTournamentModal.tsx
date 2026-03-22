import { useState, useEffect, FormEvent } from 'react';
import { X, Trophy, MapPin, Settings2, ChevronRight } from 'lucide-react';
import api from '../../api/client';

interface AddTournamentModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSuccess: () => void;
    seasonName: string;
}

export const AddTournamentModal = ({ isOpen, onClose, seasonName, onSuccess }: AddTournamentModalProps) => {
    // --- State ---
    const [name, setName] = useState('');
    const [selectedCourse, setSelectedCourse] = useState('');
    const [courses, setCourses] = useState<any[]>([]);
    const [strategy, setStrategy] = useState('STABLEFORD');
    const [multiplier, setMultiplier] = useState(1.0);
    const [pointsMap, setPointsMap] = useState<Record<string, number>>({
        "-2": 4,
        "-1": 3,
        "0": 2,
        "1": 1,
        "2": 0
    });
    
    // Leaderboard state within the modal (pulls from backend existing data)
    const [showLeaderboard, setShowLeaderboard] = useState(false);
    const [lbLoading, setLbLoading] = useState(false);
    const [lbError, setLbError] = useState<string | null>(null);
    const [leaderboard, setLeaderboard] = useState<any[] | null>(null);

    // --- Effects ---
    useEffect(() => {
        if (isOpen) {
            fetchCourses();
        }
    }, [isOpen]);

    const fetchCourses = async () => {
        try {
            const res = await api.get('/courses');
            setCourses(res.data);
        } catch (err) {
            console.error("Failed to fetch courses:", err);
        }
    };

    if (!isOpen) return null;

    // --- Handlers ---
    const handlePointChange = (key: string, val: string) => {
        setPointsMap(prev => ({
            ...prev,
            [key]: parseInt(val) || 0
        }));
    };

    const fetchLeaderBoard = async () => {
        if (!seasonName || !name) return;
        setLbLoading(true);
        setLbError(null);
        try {
            const res = await api.get(`/tournaments/${encodeURIComponent(seasonName)}/${encodeURIComponent(name)}/leaderBoard`);
            setLeaderboard(res.data || []);
        } catch (err: any) {
            const message = err?.response?.data?.message || 'Failed to load leaderboard';
            setLbError(message);
            setLeaderboard(null);
        } finally {
            setLbLoading(false);
        }
    };

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();

        // Match the TournamentCreateRequest DTO exactly
        const request = {
            name,
            season_name: seasonName,
            strategy_type: strategy,
            courseName: selectedCourse,
            pointsMap: Object.fromEntries(
                Object.entries(pointsMap).map(([k, v]) => [parseInt(k), v])
            ),
            handicapMultiplier: multiplier
        };

        try {
            await api.post('/tournaments/start', request);
            onSuccess();
            onClose();
        } catch (err) {
            console.error("Tournament start failed:", err);
            alert("Failed to start tournament. Ensure all fields are filled.");
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-latte-text/20 backdrop-blur-sm p-4">
            <div className="w-full max-w-2xl bg-white rounded-3xl shadow-2xl border border-latte-crust flex flex-col max-h-[90vh]">

                {/* Header */}
                <div className="p-8 border-b border-latte-base flex justify-between items-center">
                    <div>
                        <h3 className="text-2xl font-black flex items-center gap-2 text-latte-mauve">
                            <Trophy size={28} /> New Tournament
                        </h3>
                        <p className="text-xs font-bold text-latte-subtext uppercase tracking-widest mt-1">
                            Season: {seasonName}
                        </p>
                    </div>
                    <button onClick={onClose} className="text-latte-subtext hover:text-latte-red transition-colors"><X /></button>
                </div>

                <form onSubmit={handleSubmit} className="p-8 space-y-8 overflow-y-auto">

                    {/* Course & Name Row */}
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        <div className="space-y-2">
                            <label className="text-xs font-black uppercase tracking-widest text-latte-subtext ml-1">Tournament Name</label>
                            <input
                                className="w-full p-4 rounded-2xl border border-latte-crust outline-none focus:ring-2 focus:ring-latte-mauve bg-latte-base/10"
                                placeholder="e.g. Pinehurst Open"
                                value={name}
                                onChange={e => setName(e.target.value)}
                                required
                            />
                        </div>
                        <div className="space-y-2">
                            <label className="text-xs font-black uppercase tracking-widest text-latte-subtext ml-1">Select Course</label>
                            <div className="relative">
                                <select
                                    required
                                    className="w-full p-4 rounded-2xl border border-latte-crust bg-latte-base/10 appearance-none outline-none focus:ring-2 focus:ring-latte-mauve font-bold"
                                    value={selectedCourse}
                                    onChange={e => setSelectedCourse(e.target.value)}
                                >
                                    <option value="">-- Choose Course --</option>
                                    {courses.map(course => (
                                        <option key={course.name} value={course.name}>{course.name}</option>
                                    ))}
                                </select>
                                <MapPin className="absolute right-4 top-1/2 -translate-y-1/2 text-latte-subtext pointer-events-none" size={18} />
                            </div>
                        </div>
                    </div>

                    {/* Strategy & Multiplier Row */}
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        <div className="space-y-2">
                            <label className="text-xs font-black uppercase tracking-widest text-latte-subtext ml-1">Scoring Strategy</label>
                            <select
                                className="w-full p-4 rounded-2xl border border-latte-crust bg-white font-bold"
                                value={strategy}
                                onChange={e => setStrategy(e.target.value)}
                            >
                                <option value="STABLEFORD">Stableford</option>
                                <option value="STROKEPLAY">Stroke Play (Gross)</option>
                            </select>
                        </div>
                        <div className="space-y-2">
                            <label className="text-xs font-black uppercase tracking-widest text-latte-subtext flex justify-between ml-1">
                                HCP Multiplier <span>{Math.round(multiplier * 100)}%</span>
                            </label>
                            <input
                                type="range" min="0.5" max="1.5" step="0.05"
                                className="w-full h-10 accent-latte-mauve cursor-pointer"
                                value={multiplier}
                                onChange={e => setMultiplier(parseFloat(e.target.value))}
                            />
                        </div>
                    </div>

                    {/* Points Map Section */}
                    <div className="bg-latte-base/30 p-6 rounded-3xl border border-latte-crust">
                        <div className="flex justify-between items-center mb-6">
                            <h4 className="text-xs font-black text-latte-mauve uppercase tracking-widest flex items-center gap-2">
                                <Settings2 size={16} /> Points Distribution
                            </h4>
                            <button
                                type="button"
                                onClick={() => setPointsMap({ "-2": 4, "-1": 3, "0": 2, "1": 1, "2": 0 })}
                                className="text-[10px] font-black uppercase text-latte-subtext hover:text-latte-mauve transition-colors"
                            >
                                Reset to Standard
                            </button>
                        </div>

                        <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
                            {[
                                { key: "-2", label: "Eagle" },
                                { key: "-1", label: "Birdie" },
                                { key: "0",  label: "Par" },
                                { key: "1",  label: "Bogey" },
                                { key: "2",  label: "Dbl +" }
                            ].map((item) => (
                                <div key={item.key} className="space-y-1">
                                    <label className="text-[9px] font-black text-latte-subtext uppercase text-center block">
                                        {item.label}
                                    </label>
                                    <input
                                        type="number"
                                        className="w-full p-3 bg-white rounded-xl border border-latte-crust text-sm font-black text-center focus:border-latte-mauve outline-none shadow-sm"
                                        value={pointsMap[item.key]}
                                        onChange={(e) => handlePointChange(item.key, e.target.value)}
                                    />
                                </div>
                            ))}
                        </div>
                    </div>

                    {/* Leaderboard (Existing Tournament) */}
                    <div className="space-y-3">
                        <div className="flex items-center justify-between">
                            <div>
                                <h4 className="text-xs font-black uppercase tracking-widest text-latte-subtext ml-1">Tournament Leaderboard</h4>
                                <p className="text-[10px] text-latte-subtext ml-1">Looks up an existing tournament in this season by the entered name.</p>
                            </div>
                            <button
                                type="button"
                                onClick={() => {
                                    const willOpen = !showLeaderboard;
                                    setShowLeaderboard(willOpen);
                                    if (willOpen && name) {
                                        fetchLeaderBoard();
                                    }
                                }}
                                disabled={!name}
                                className={`px-4 py-2 text-sm font-bold rounded-xl flex items-center gap-2 border ${!name ? 'opacity-50 cursor-not-allowed bg-latte-mantle text-latte-subtext' : 'bg-latte-base hover:bg-latte-crust'}`}
                            >
                                <ChevronRight size={16} className={`${showLeaderboard ? 'rotate-90' : ''} transition-transform`} /> View Leaderboard
                            </button>
                        </div>

                        {showLeaderboard && (
                            <div className="border border-latte-crust rounded-2xl overflow-hidden">
                                <div className="bg-latte-base px-4 py-2 flex items-center gap-2 text-latte-subtext text-xs uppercase font-black">
                                    <Trophy size={14} /> Leaderboard for: <span className="text-latte-text">{name}</span>
                                </div>
                                <div className="p-4">
                                    {lbLoading && (
                                        <div className="text-sm text-latte-subtext font-bold">Loading...</div>
                                    )}
                                    {lbError && (
                                        <div className="text-sm text-latte-red font-bold">{lbError}</div>
                                    )}
                                    {!lbLoading && !lbError && (
                                        leaderboard && leaderboard.length > 0 ? (
                                            <>
                                                <div className="grid grid-cols-12 text-xs font-black text-latte-subtext uppercase mb-2">
                                                    <div className="col-span-6">Player</div>
                                                    <div className="col-span-3 text-center">Score</div>
                                                    <div className="col-span-3 text-center">Birdies</div>
                                                </div>
                                                <div className="divide-y divide-latte-crust">
                                                    {leaderboard.map((lb: any, i: number) => (
                                                        <div key={i} className="grid grid-cols-12 py-2 items-center">
                                                            <div className="col-span-6 font-bold text-latte-text">{lb.playerName}</div>
                                                            <div className="col-span-3 text-center font-black text-latte-mauve">{lb.score}</div>
                                                            <div className="col-span-3 text-center text-latte-subtext">{lb.birdies}</div>
                                                        </div>
                                                    ))}
                                                </div>
                                            </>
                                        ) : (
                                            <div className="text-sm text-latte-subtext font-bold">No entries yet.</div>
                                        )
                                    )}
                                </div>
                            </div>
                        )}
                    </div>

                    <button type="submit" className="w-full py-5 bg-latte-mauve text-white rounded-2xl font-black shadow-xl hover:brightness-110 active:scale-95 transition-all text-lg uppercase tracking-widest">
                        Initialize Tournament
                    </button>
                </form>
            </div>
        </div>
    );
};