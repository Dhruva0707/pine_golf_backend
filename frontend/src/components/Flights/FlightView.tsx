import { useState, useEffect } from 'react';
import { jwtDecode } from 'jwt-decode';
import { Send, History, Plus, Calendar, Target, User, Search, X, Trash2, Link2 } from 'lucide-react';
import api from '../../api/client';

interface FlightScoreDTO {
    playerName: string;
    score: number;
    birdies: number;
    holeScores: number[];
    courseName: string;
}

interface FlightDTO {
    id?: number;
    date: string;
    flights: FlightScoreDTO[];
}

interface TournamentDTO {
    id: number;
    name: string;
    seasonName?: string;
}

export const FlightsView = ({ isAdmin }: { isAdmin: boolean }) => {
    const [view, setView] = useState<'history' | 'add'>('history');
    const [flights, setFlights] = useState<FlightDTO[]>([]);
    const [searchPlayer, setSearchPlayer] = useState('');
    const [loading, setLoading] = useState(false);
    const [players, setPlayers] = useState<string[]>([]);
    const [courses, setCourses] = useState<any[]>([]);
    const [currentUserName, setCurrentUserName] = useState<string | null>(null);
    const [expandedFlights, setExpandedFlights] = useState<Record<string, boolean>>({});

        // Admin link-to-tournament state
        const [linkingFlightId, setLinkingFlightId] = useState<number | null>(null);
        const [activeTournaments, setActiveTournaments] = useState<TournamentDTO[]>([]);
        const [tournamentsLoading, setTournamentsLoading] = useState(false);
        const [tournamentsError, setTournamentsError] = useState<string | null>(null);
        const [selectedTournamentId, setSelectedTournamentId] = useState<number | ''>('');

    // Form State for New Flight (support multiple players like AddFlightModal)
    const [courseName, setCourseName] = useState('');
    const [flightRows, setFlightRows] = useState<{ playerName: string; scores: number[] }[]>([]);
    const [selectedPlayerToAdd, setSelectedPlayerToAdd] = useState('');

    // Fetch Logic
    const fetchFlights = async (playerName?: string) => {
        setLoading(true);
        try {
            // If playerName exists, hit the specific player endpoint, else hit global flights
            const url = playerName
                ? `/players/${encodeURIComponent(playerName)}/flights`
                : `/flights/all`;

            const res = await api.get(url);

            // Sort latest first (Descending by date)
            const sorted = res.data.sort((a: FlightDTO, b: FlightDTO) =>
                new Date(b.date).getTime() - new Date(a.date).getTime()
            );
            setFlights(sorted);
        } catch (err) {
            console.error("Fetch failed", err);
            setFlights([]);
        } finally {
            setLoading(false);
        }
    };

    const fetchPlayers = async () => {
        try {
            const res = await api.get('/players');
            const names: string[] = (res.data || []).map((p: any) => p.name ?? p.username ?? p.displayName).filter(Boolean);
            const uniqueNames = Array.from(new Set(names)).sort((a, b) => a.localeCompare(b));
            setPlayers(uniqueNames);
        } catch (err) {
            console.error('Failed to fetch players', err);
            setPlayers([]);
        }
    };

    const fetchCourses = async () => {
        try {
            const res = await api.get('/courses');
            setCourses(Array.isArray(res.data) ? res.data : []);
        } catch (err) {
            console.error('Failed to fetch courses', err);
            setCourses([]);
        }
    };

    useEffect(() => {
        // Initial load: try to default to signed-in user
        const token = localStorage.getItem('golf_token');
        const doInitialFetches = (name?: string) => {
            if (name) {
                setSearchPlayer(name);
                fetchFlights(name);
            } else {
                fetchFlights();
            }
            fetchPlayers();
            fetchCourses();
        };
        if (token) {
            try {
                const decoded: any = jwtDecode(token);
                const name = decoded?.sub as string | undefined;
                setCurrentUserName(name ?? null);
                doInitialFetches(name);
            } catch (e) {
                console.warn('Failed to decode token', e);
                setCurrentUserName(null);
                doInitialFetches();
            }
        } else {
            doInitialFetches();
        }
    }, []);

    const handleSearch = (e: React.FormEvent) => {
        e.preventDefault();
        setExpandedFlights({});
        fetchFlights(searchPlayer);
    };

    const clearSearch = () => {
        setSearchPlayer('');
        setExpandedFlights({});
        fetchFlights();
    };

    const handleSubmitFlight = async () => {
        if (!courseName) return alert("Course is required");
        const courseExists = courses.some((c: any) => c?.name === courseName);
        if (!courseExists) return alert("Please select a valid course from the list.");
        if (flightRows.length === 0) return alert("Please add at least one player to this flight.");

        const payload: FlightScoreDTO[] = flightRows.map(r => {
            const totalScore = r.scores.reduce((a, b) => a + (Number(b) || 0), 0);
            return {
                playerName: r.playerName,
                courseName,
                holeScores: r.scores,
                score: totalScore,
                birdies: 0
            } as FlightScoreDTO;
        });

        try {
            await api.post('/flights', payload);
            alert("Flight saved!");
            setCourseName('');
            setFlightRows([]);
            setSelectedPlayerToAdd('');
            setView('history');
            fetchFlights();
        } catch (err) { alert("Error saving flight."); }
    };

    // When switching to add view, default scores for any new players and optionally add current user
    useEffect(() => {
        if (view === 'add') {
            // Auto-add signed-in user once if available and no players yet
            if (flightRows.length === 0 && currentUserName && players.includes(currentUserName)) {
                setFlightRows([{ playerName: currentUserName, scores: Array(18).fill(3) }]);
            }
        }
    }, [view]);

    // If players or current user info arrives later, ensure default target player in Add view
    useEffect(() => {
        if (view === 'add' && flightRows.length === 0 && currentUserName && players.includes(currentUserName)) {
            setFlightRows([{ playerName: currentUserName, scores: Array(18).fill(3) }]);
        }
    }, [players, currentUserName]);

    // Helpers for multi-player add view
    const addPlayerToFlight = (name: string) => {
        if (!name) return;
        if (flightRows.some(r => r.playerName === name)) return;
        setFlightRows(prev => [...prev, { playerName: name, scores: Array(18).fill(3) }]);
        setSelectedPlayerToAdd('');
    };
    const removePlayerFromFlight = (index: number) => {
        setFlightRows(prev => prev.filter((_, i) => i !== index));
    };

    const toggleFlightExpand = (key: string) => {
        setExpandedFlights(prev => ({ ...prev, [key]: !prev[key] }));
    };

    // Admin: open link panel and fetch active tournaments
    const openLinkPanel = async (flightId: number | undefined) => {
        if (!flightId) return;
        setLinkingFlightId(flightId);
        setSelectedTournamentId('');
        setTournamentsError(null);
        setTournamentsLoading(true);
        try {
            const res = await api.get('/tournaments/active');
            setActiveTournaments(Array.isArray(res.data) ? res.data : []);
        } catch (e: any) {
            console.error('Failed to load active tournaments', e);
            setTournamentsError('Failed to load active tournaments');
            setActiveTournaments([]);
        } finally {
            setTournamentsLoading(false);
        }
    };

    const cancelLinkPanel = () => {
        setLinkingFlightId(null);
        setSelectedTournamentId('');
        setTournamentsError(null);
    };

    const performLink = async () => {
        if (!linkingFlightId || !selectedTournamentId) {
            alert('Please choose a tournament');
            return;
        }
        try {
            await api.patch(`/tournaments/${selectedTournamentId}/${linkingFlightId}`);
            alert('Flight linked to tournament');
            cancelLinkPanel();
            // Optionally refresh flights list to reflect any potential changes
            fetchFlights(searchPlayer || undefined);
        } catch (e) {
            console.error('Failed to link flight', e);
            alert('Failed to link flight to tournament');
        }
    };

    return (
        <div className="space-y-6">
            {/* --- TOP HEADER & ACTIONS --- */}
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div>
                    <h2 className="text-2xl font-black text-latte-text">Flight Management</h2>
                    <p className="text-xs font-bold text-latte-subtext uppercase tracking-widest">
                        {view === 'history' ? 'Review Scoring History' : 'Enter New Scorecard'}
                    </p>
                </div>

                <div className="flex items-center gap-3">
                    <button
                        onClick={() => setView(view === 'history' ? 'add' : 'history')}
                        className={`flex items-center gap-2 px-5 py-2.5 rounded-xl font-black text-sm transition-all shadow-sm ${
                            view === 'add'
                                ? 'bg-latte-base text-latte-text border border-latte-crust'
                                : 'bg-latte-green text-white hover:opacity-90'
                        }`}
                    >
                        {view === 'history' ? <><Plus size={18}/> Add Flight</> : <><History size={18}/> View History</>}
                    </button>
                </div>
            </div>

            {/* --- SEARCH BAR (Only show in history view) --- */}
            {view === 'history' && (
                <form onSubmit={handleSearch} className="relative group max-w-md">
                    <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-latte-subtext group-focus-within:text-latte-mauve transition-colors" size={18} />
                    <input
                        type="text"
                        list="player-list"
                        placeholder="Search by Player Name..."
                        value={searchPlayer}
                        onChange={(e) => {
                            const val = e.target.value;
                            setSearchPlayer(val);
                            if (players.includes(val)) {
                                setExpandedFlights({});
                                fetchFlights(val);
                            }
                        }}
                        className="w-full bg-white border border-latte-crust rounded-2xl py-3 pl-12 pr-12 font-bold shadow-sm focus:ring-2 ring-latte-mauve outline-none transition-all"
                    />
                    <datalist id="player-list">
                        {players.map((p) => (
                            <option key={p} value={p} />
                        ))}
                    </datalist>
                    {searchPlayer && (
                        <button
                            type="button"
                            onClick={clearSearch}
                            className="absolute right-4 top-1/2 -translate-y-1/2 text-latte-subtext hover:text-latte-red"
                        >
                            <X size={18} />
                        </button>
                    )}
                </form>
            )}

            {/* --- CONTENT AREA --- */}
            {view === 'history' ? (
                <div className="grid gap-4">
                    {loading ? (
                        <div className="text-center py-10 text-latte-subtext animate-pulse font-bold">Loading rounds...</div>
                    ) : flights.length === 0 ? (
                        <div className="bg-white rounded-3xl p-16 text-center border border-latte-crust">
                            <p className="text-latte-subtext font-bold">No flights found.</p>
                        </div>
                    ) : (
                        flights.map((f, i) => {
                            // Only show flights that have at least one available player entry (and respect search filter)
                            const allEntries = (f.flights || []).filter(fs => fs && fs.playerName);
                            const containsSelected = searchPlayer ? allEntries.some(fs => fs.playerName === searchPlayer) : true;
                            if (!containsSelected || allEntries.length === 0) return null;

                            const course = allEntries[0]?.courseName || '';
                            const flightKey = String(f.id ?? `${f.date}|${course}`);
                            const isExpanded = !!expandedFlights[flightKey];
                            const entries = (searchPlayer && !isExpanded)
                                ? allEntries.filter(fs => fs.playerName === searchPlayer)
                                : allEntries;

                            return (
                                <div key={i} className="bg-white rounded-2xl border border-latte-crust shadow-sm overflow-hidden hover:border-latte-mauve transition-colors">
                                    {/* Top header with Course on the left and Date on the right */}
                                    <div
                                        className={`bg-latte-mantle/50 px-6 py-3 flex justify-between items-center border-b border-latte-crust ${searchPlayer ? 'cursor-pointer hover:bg-latte-mantle/70' : ''}`}
                                        onClick={() => { if (searchPlayer) toggleFlightExpand(flightKey); }}
                                        role={searchPlayer ? 'button' as any : undefined}
                                        aria-expanded={searchPlayer ? isExpanded : undefined}
                                        title={searchPlayer ? (isExpanded ? 'Hide other players' : 'Show all players in this flight') : undefined}
                                    >
                                        <div className="flex items-center gap-3">
                                            <div className="bg-white p-2 rounded-lg shadow-sm text-latte-mauve">
                                                <Target size={16} />
                                            </div>
                                            <span className="font-black text-latte-text">{course}</span>
                                        </div>
                                        <div className="flex items-center gap-2">
                                            <span className="text-[10px] font-black text-latte-subtext uppercase flex items-center gap-1">
                                                <Calendar size={12} /> {new Date(f.date).toLocaleDateString()}
                                            </span>
                                            {searchPlayer && (
                                                <span className="text-[9px] font-black text-latte-subtext px-2 py-0.5 border border-latte-crust rounded-lg">
                                                    {isExpanded ? 'Showing all players' : 'Show all players'}
                                                </span>
                                            )}
                                            {isAdmin && (
                                                <button
                                                    type="button"
                                                    onClick={(e) => { e.stopPropagation(); openLinkPanel(f.id); }}
                                                    className="ml-2 flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-black bg-latte-blue text-white hover:opacity-90"
                                                    title="Link this flight to an active tournament"
                                                >
                                                    <Link2 size={14} /> Link
                                                </button>
                                            )}
                                        </div>
                                    </div>

                                    {/* Entries list */}
                                    <div className="p-6 space-y-6">
                                        {entries.map((fs, idx) => (
                                            <div key={idx} className="border border-latte-crust/60 rounded-xl p-4">
                                                <div className="flex items-center justify-between mb-3">
                                                    <div className="flex items-center gap-3">
                                                        <div className="bg-white p-2 rounded-lg shadow-sm text-latte-mauve"><User size={16} /></div>
                                                        <span className="font-black text-latte-text">{fs.playerName}</span>
                                                    </div>
                                                    <div className="flex items-center gap-4">
                                                        {typeof fs.birdies === 'number' && (
                                                            <div className="bg-latte-green/10 text-latte-green px-3 py-1 rounded-lg text-xs font-black">{fs.birdies} Birdies</div>
                                                        )}
                                                        <div className="bg-latte-mauve/10 px-4 py-2 rounded-xl text-right">
                                                            <span className="text-2xl font-black text-latte-mauve block leading-none">{fs.score}</span>
                                                            <span className="text-[8px] font-black uppercase text-latte-mauve/60">Total Score</span>
                                                        </div>
                                                    </div>
                                                </div>

                                                {/* Hole scores grid */}
                                                <div className="grid grid-cols-9 md:grid-cols-18 gap-1">
                                                    {(fs.holeScores || []).map((s, hIdx) => (
                                                        <div key={hIdx} className="text-center">
                                                            <div className="text-[8px] font-bold text-latte-subtext mb-1">{hIdx + 1}</div>
                                                            <div className="bg-latte-base rounded-md py-1.5 text-xs font-black text-latte-text border border-latte-crust/50">{s}</div>
                                                        </div>
                                                    ))}
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            );
                        })
                    )}
                </div>
            ) : (
                <div className="bg-white p-8 rounded-3xl border border-latte-crust shadow-sm max-w-6xl mx-auto">
                    <div className="grid grid-cols-2 gap-4 mb-6">
                        <div>
                            <label className="block text-xs font-black uppercase text-latte-subtext mb-2 ml-1">Course</label>
                            <select
                                value={courseName}
                                onChange={(e) => setCourseName(e.target.value)}
                                className="w-full bg-latte-mantle border-transparent rounded-xl px-4 py-3 font-bold focus:ring-2 ring-latte-mauve outline-none transition-all"
                            >
                                <option value="" disabled>Select a course...</option>
                                {courses.map((c: any) => (
                                    <option key={c.name} value={c.name}>{c.name}</option>
                                ))}
                            </select>
                        </div>
                        <div>
                            <label className="block text-xs font-black uppercase text-latte-subtext mb-2 ml-1">Add Player</label>
                            <select
                                className="w-full bg-latte-mantle border-transparent rounded-xl px-4 py-3 font-bold focus:ring-2 ring-latte-mauve outline-none transition-all"
                                value={selectedPlayerToAdd}
                                onChange={(e) => addPlayerToFlight(e.target.value)}
                            >
                                <option value="">+ Add Player to this Flight...</option>
                                {players
                                    .filter(p => !flightRows.find(r => r.playerName === p))
                                    .map(p => (
                                        <option key={p} value={p}>{p}</option>
                                    ))}
                            </select>
                        </div>
                    </div>

                    <div className="overflow-x-auto w-full bg-latte-base/40 rounded-2xl border border-latte-crust shadow-inner p-4">
                        <table className="w-full text-left border-collapse min-w-[900px]">
                            <thead>
                                <tr className="text-[10px] font-black text-latte-subtext uppercase tracking-widest border-b border-latte-crust">
                                    <th className="p-2 w-10 text-center"></th>
                                    <th className="p-2">Player</th>
                                    {Array.from({ length: 18 }).map((_, i) => (
                                        <th key={i} className="p-2 text-center w-8 text-[9px]">H{i + 1}</th>
                                    ))}
                                    <th className="p-2 text-right">Total</th>
                                </tr>
                            </thead>
                            <tbody>
                                {flightRows.map((row, rowIndex) => (
                                    <tr key={rowIndex} className="border-b border-latte-mantle hover:bg-white/30 transition-colors">
                                        <td className="p-2 text-center">
                                            <button onClick={() => removePlayerFromFlight(rowIndex)} className="text-latte-subtext hover:text-latte-red">
                                                <Trash2 size={14} />
                                            </button>
                                        </td>
                                        <td className="p-2 font-bold text-sm whitespace-nowrap">{row.playerName}</td>
                                        {row.scores.map((score, holeIndex) => (
                                            <td key={holeIndex} className="p-0.5">
                                                <input
                                                    type="number"
                                                    className="w-full min-w-[32px] text-center p-1 rounded-md border border-transparent hover:border-latte-crust text-xs font-bold focus:border-latte-mauve focus:bg-white outline-none bg-transparent"
                                                    value={score || ''}
                                                    onChange={(e) => {
                                                        const next = [...flightRows];
                                                        next[rowIndex].scores[holeIndex] = parseInt(e.target.value) || 0;
                                                        setFlightRows(next);
                                                    }}
                                                />
                                            </td>
                                        ))}
                                        <td className="p-2 text-right font-black text-latte-mauve">
                                            {row.scores.reduce((a, b) => a + (Number(b) || 0), 0)}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>

                        {flightRows.length === 0 && (
                            <div className="text-center text-latte-subtext text-sm py-6">No players added yet. Use the Add Player dropdown above.</div>
                        )}
                    </div>

                    <button
                        onClick={handleSubmitFlight}
                        disabled={!courseName || flightRows.length === 0}
                        className="mt-6 w-full bg-latte-mauve text-white py-4 rounded-2xl font-black uppercase tracking-widest flex items-center justify-center gap-3 hover:shadow-lg hover:opacity-95 transition-all disabled:opacity-50"
                    >
                        <Send size={20} /> Submit Flight
                    </button>
                </div>
            )}
            {isAdmin && linkingFlightId !== null && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" onClick={cancelLinkPanel}>
                    <div className="bg-white rounded-2xl shadow-xl border border-latte-crust w-full max-w-md" onClick={(e) => e.stopPropagation()}>
                        <div className="flex items-center justify-between px-5 py-3 border-b border-latte-crust">
                            <h3 className="font-black text-latte-text text-lg">Link Flight #{linkingFlightId} to Tournament</h3>
                            <button onClick={cancelLinkPanel} className="text-latte-subtext hover:text-latte-red"><X size={18} /></button>
                        </div>
                        <div className="p-5 space-y-4">
                            {tournamentsLoading ? (
                                <div className="text-sm text-latte-subtext">Loading active tournaments...</div>
                            ) : tournamentsError ? (
                                <div className="text-sm text-latte-red">{tournamentsError}</div>
                            ) : activeTournaments.length === 0 ? (
                                <div className="text-sm text-latte-subtext">No active tournaments found.</div>
                            ) : (
                                <div>
                                    <label className="block text-xs font-black uppercase text-latte-subtext mb-2 ml-1">Active Tournaments</label>
                                    <select
                                        className="w-full bg-latte-mantle border-transparent rounded-xl px-4 py-3 font-bold focus:ring-2 ring-latte-mauve outline-none transition-all"
                                        value={selectedTournamentId}
                                        onChange={(e) => setSelectedTournamentId(Number(e.target.value) as number)}
                                    >
                                        <option value="">Select a tournament...</option>
                                        {activeTournaments.map(t => (
                                            <option key={t.id} value={t.id}>
                                                {t.name}{t.seasonName ? ` (${t.seasonName})` : ''}
                                            </option>
                                        ))}
                                    </select>
                                </div>
                            )}
                        </div>
                        <div className="px-5 pb-5 flex items-center justify-end gap-2">
                            <button onClick={cancelLinkPanel} className="px-4 py-2 rounded-xl font-bold text-sm border border-latte-crust text-latte-text bg-white hover:bg-latte-base">Cancel</button>
                            <button onClick={performLink} disabled={!selectedTournamentId} className="px-4 py-2 rounded-xl font-bold text-sm bg-latte-green text-white disabled:opacity-50">Link Flight</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};