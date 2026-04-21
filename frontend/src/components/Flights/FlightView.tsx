import { useState, useEffect } from 'react';
import type { FormEvent } from 'react';
import {Send, History, Plus, Calendar, User, Search, X, Trash2, Link} from 'lucide-react';
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

interface PlayerDTO {
    id?: number;
    name: string;
    team?: string;
    handicap: number;
}

export const FlightsView = ({ isAdmin, currentUserName }: { isAdmin: boolean; currentUserName?: string | null }) => {
    const [view, setView] = useState<'history' | 'add'>('history');
    const [flights, setFlights] = useState<FlightDTO[]>([]);
    const [searchPlayer, setSearchPlayer] = useState('');
    const [loading, setLoading] = useState(false);
    const [players, setPlayers] = useState<PlayerDTO[]>([]);
    const [courses, setCourses] = useState<any[]>([]);
    const [expandedFlights, setExpandedFlights] = useState<Record<string, boolean>>({});

    // Form State for New Flight (support multiple players like AddFlightModal)
    const [courseName, setCourseName] = useState('');
    // Each row now holds expected and actual values separately. Actual uses null for un-entered values.
    const [flightRows, setFlightRows] = useState<{ player: PlayerDTO; expected: number[]; actual: (number | null)[] }[]>([]);
    const [selectedPlayerToAdd, setSelectedPlayerToAdd] = useState('');

    // --- New state for admin assignment flow ---
    const [isAssignModalOpen, setIsAssignModalOpen] = useState(false);
    const [activeFlightToAssign, setActiveFlightToAssign] = useState<FlightDTO | null>(null);
    const [activeTournaments, setActiveTournaments] = useState<any[] | null>(null);
    const [assigning, setAssigning] = useState(false);
    const [tournamentsLoading, setTournamentsLoading] = useState(false);

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
            console.log('FlightsView: fetched flights', sorted);
        } catch (err) {
            console.error("Fetch failed", err);
            setFlights([]);
        } finally {
            setLoading(false);
        }
    };

    const normalizePlayer = (p: any): PlayerDTO | null => {
        const name = p?.name ?? p?.username ?? p?.displayName;
        if (!name) return null;
        return {
            id: p?.id ?? p?.playerId,
            name,
            team: p?.team,
            handicap: Number(p.handicap)
        };
    };

    const fetchPlayers = async () => {
        try {
            const res = await api.get('/players');
            const normalized = (res.data || []).map(normalizePlayer).filter(Boolean) as PlayerDTO[];
            const uniqueByName = Array.from(new Map(normalized.map(p => [p.name, p])).values()).sort((a, b) => a.name.localeCompare(b.name));
            setPlayers(uniqueByName);
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

    const getDefaultScoresForCourse = (name: string) => {
        const course = courses.find((c: any) => c?.name === name);
        const pars = course?.pars ?? [];
        return Array.from({ length: 18 }, (_, i) => Number(pars[i]) || 3);
    };

    const getExpectedScoresForPlayer = async (player: PlayerDTO | undefined, courseName: string) => {
        const course = courses.find((c: any) => c?.name === courseName);
        const courseId = course?.id;
        const playerId = player?.id;

        if (courseId !== undefined && playerId !== undefined) {
            try {
                const res = await api.get(`/flights/${courseId}/${playerId}`);
                if (Array.isArray(res.data) && res.data.length > 0) return res.data.map((v: any) => Number(v) || 0);
            } catch (err) {
                console.error('Failed to fetch expected scores for player', err);
            }
        }

        return getDefaultScoresForCourse(courseName);
    };

    useEffect(() => {
        // Initial load: use the passed currentUserName (from Dashboard) if present
        const doInitialFetches = (name?: string | null) => {
            if (name) {
                setSearchPlayer(name);
                fetchFlights(name);
            } else {
                fetchFlights();
            }
            fetchPlayers();
            fetchCourses();
        };

        doInitialFetches(currentUserName ?? undefined);
    }, [currentUserName]);

    const handleSearch = (e: FormEvent) => {
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
            const holeScores = r.actual.map((a, idx) => (a !== null && !isNaN(a as number) ? (a as number) : r.expected[idx]));
            const totalScore = holeScores.reduce((a, b) => a + (Number(b) || 0), 0);
            return {
                playerName: r.player.name,
                courseName,
                holeScores,
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
            if (flightRows.length === 0 && currentUserName) {
                const current = players.find(p => p.name === currentUserName);
                if (current) {
                    void (async () => {
                        const expected = await getExpectedScoresForPlayer(current, courseName);
                        setFlightRows([{ player: current, expected, actual: Array(18).fill(null) }]);
                    })();
                }
            }
        }
    }, [view, courseName, flightRows.length, currentUserName, players]);

    // If players or current user info arrives later, ensure default target player in Add view
    useEffect(() => {
        if (view === 'add' && flightRows.length === 0 && currentUserName) {
            const current = players.find(p => p.name === currentUserName);
            if (current) {
                void (async () => {
                    const expected = await getExpectedScoresForPlayer(current, courseName);
                    setFlightRows([{ player: current, expected, actual: Array(18).fill(null) }]);
                })();
            }
        }
    }, [players, currentUserName]);

    // Helpers for multi-player add view
    const addPlayerToFlight = async (name: string) => {
        if (!name) return;
        if (flightRows.some(r => r.player.name === name)) return;
        const player = players.find(p => p.name === name);
        if (!player) return;
        const expected = await getExpectedScoresForPlayer(player, courseName);
        setFlightRows(prev => [...prev, { player, expected, actual: Array(18).fill(null) }]);
        setSelectedPlayerToAdd('');
    };
    const removePlayerFromFlight = (index: number) => {
        setFlightRows(prev => prev.filter((_, i) => i !== index));
    };

    const toggleFlightExpand = (key: string) => {
        setExpandedFlights(prev => ({ ...prev, [key]: !prev[key] }));
    };

    // --- Admin: Assign flight to tournament flow ---
    const openAssignModal = async (flight: FlightDTO) => {
        setActiveFlightToAssign(flight);
        setIsAssignModalOpen(true);
        // load tournaments if not loaded yet
        if (activeTournaments === null) {
            setTournamentsLoading(true);
            try {
                const res = await api.get('/tournaments');
                let tournaments = res.data || [];

                // collect season identifiers (could be numeric ids or names)
                const seasonKeys = Array.from(new Set(
                    tournaments.map((t: any) => (t?.seasonId ?? t?.seasonId === 0 ? t.seasonId : (t?.season || t?.seasonName))).filter(Boolean)
                ));

                const seasonNameMap: Record<string, string> = {};

                await Promise.all(seasonKeys.map(async (rawKey: any) => {
                    const keyStr = String(rawKey);
                    // Try to fetch by numeric id first (SeasonController supports GET /seasons/{id} where id is Long)
                    try {
                        const sres = await api.get(`/seasons/${keyStr}`);
                        seasonNameMap[keyStr] = sres.data?.name ?? keyStr;
                        return;
                    } catch (e: any) {
                        // If 404 or parsing issues, fall back to checking by name via /seasons list
                        if (e?.response?.status && e.response.status !== 404) {
                            console.warn('Error fetching season by id', keyStr, e);
                        }
                    }

                    // Fallback: load all seasons and try to match by name
                    try {
                        const listRes = await api.get('/seasons');
                        const names: string[] = Array.isArray(listRes.data) ? listRes.data : [];
                        if (names.includes(keyStr)) {
                            seasonNameMap[keyStr] = keyStr;
                        } else {
                            seasonNameMap[keyStr] = '';
                        }
                    } catch (e) {
                        console.warn('Failed to fetch seasons list for fallback', e);
                        seasonNameMap[keyStr] = '';
                    }
                }));

                // Attach seasonName to each tournament for display; respect any existing seasonName/season fields
                tournaments = tournaments.map((t: any) => ({
                    ...t,
                    seasonName: t.seasonName || t.season || (t.seasonId !== undefined ? seasonNameMap[String(t.seasonId)] : (t.season || t.seasonName || ''))
                }));

                setActiveTournaments(tournaments);
            } catch (err) {
                console.error('Failed to load tournaments', err);
                setActiveTournaments([]);
                alert('Failed to load active tournaments');
            } finally {
                setTournamentsLoading(false);
            }
        }
    };

    const closeAssignModal = () => {
        setIsAssignModalOpen(false);
        setActiveFlightToAssign(null);
    };

    const assignFlightToTournament = async (tournamentId: number) => {
        if (!activeFlightToAssign?.id) return;
        setAssigning(true);
        try {
            await api.patch(`/flights/${activeFlightToAssign.id}/${tournamentId}/link`);
            alert('Flight assigned to tournament');
            // refresh flights for current search
            fetchFlights(searchPlayer || undefined);
            closeAssignModal();
        } catch (err) {
            console.error('Failed to assign flight', err);
            alert('Failed to assign flight to tournament');
        } finally {
            setAssigning(false);
        }
    };

    return (
        <div className="space-y-6">
            {/* --- TOP HEADER & ACTIONS --- */}
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div>
                    <h2 className="text-2xl font-black text-latte-text">ScoreCards</h2>
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
                        {view === 'history' ? <><Plus size={18}/> Add Score</> : <><History size={18}/> View History</>}
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
                            if (players.some(p => p.name === val)) {
                                setExpandedFlights({});
                                fetchFlights(val);
                            }
                        }}
                        className="w-full bg-white border border-latte-crust rounded-2xl py-3 pl-12 pr-12 font-bold shadow-sm focus:ring-2 ring-latte-mauve outline-none transition-all"
                    />
                    <datalist id="player-list">
                        {players.map((p) => (
                            <option key={p.name} value={p.name} />
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
                            const allEntries = (f.flights || []).filter(fs => fs && fs.playerName);
                            const containsSelected = searchPlayer ? allEntries.some(fs => fs.playerName === searchPlayer) : true;
                            if (!containsSelected || allEntries.length === 0) return null;

                            const course = allEntries[0]?.courseName || '';
                            const flightKey = String(f.id ?? `${f.date}|${course}`);
                            const isExpanded = expandedFlights[flightKey];
                            const entries = (searchPlayer && !isExpanded)
                                ? allEntries.filter(fs => fs.playerName === searchPlayer)
                                : allEntries;


                            return (
                                <div key={i} className="bg-white rounded-2xl border border-latte-crust shadow-sm overflow-hidden hover:border-latte-mauve transition-colors">
                                    <div
                                        className={`bg-latte-mantle/50 px-6 py-3 flex justify-between items-center border-b border-latte-crust ${searchPlayer ? 'cursor-pointer hover:bg-latte-mantle/70' : ''}`}
                                        onClick={() => { if (searchPlayer) toggleFlightExpand(flightKey); }}
                                        role={searchPlayer ? 'button' as any : undefined}
                                        aria-expanded={searchPlayer ? isExpanded : undefined}
                                        title={searchPlayer ? (isExpanded ? 'Hide other players' : 'Show all players in this flight') : undefined}
                                    >
                                        <div className="flex items-center gap-3">
                                            <div className="bg-white p-2 rounded-lg shadow-sm text-latte-mauve">
                                                <Send size={16} />
                                            </div>
                                            <span className="font-black text-latte-text">{course}</span>
                                        </div>
                                        <div className="flex items-center gap-2">
                                            <span className="text-[10px] font-black text-latte-subtext uppercase flex items-center gap-1">
                                                <Calendar size={12} /> {new Date(f.date).toLocaleDateString()}
                                            </span>

                                            {/* Admin-only assign button (only if flight has an id) */}
                                            {isAdmin && (
                                                <>
                                                    <button
                                                        onClick={(e) => { e.stopPropagation(); openAssignModal(f); }}
                                                        className="p-2 text-latte-green hover:bg-latte-mauve/10 rounded-xl transition-colors"
                                                        title="Add ScoreCard to Tournament"
                                                    >
                                                        <Link size={16} />
                                                    </button>
                                                </>
                                            )}
                                        </div>
                                    </div>

                                    <div className="p-6 space-y-6">
                                        {entries.map((fs, idx) => (
                                            <div key={idx} className="border border-latte-crust/60 rounded-xl p-4">
                                                <div className="flex items-center justify-between mb-3">
                                                    <div className="flex items-center gap-3">
                                                        <div className="bg-white p-2 rounded-lg shadow-sm text-latte-mauve"><User size={16} /></div>
                                                        <span className="font-black text-latte-text">{fs.playerName}</span>
                                                    </div>
                                                    <div className="flex items-center gap-4">
                                                        <div className="bg-latte-green/10 text-latte-green px-3 py-1 rounded-lg text-xs font-black">{fs.birdies} Birdies</div>
                                                        <div className="bg-latte-mauve/10 px-4 py-2 rounded-xl text-right">
                                                            <span className="text-2xl font-black text-latte-mauve block leading-none">{fs.score}</span>
                                                            <span className="text-[8px] font-black uppercase text-latte-mauve/60">Total Score</span>
                                                        </div>
                                                    </div>
                                                </div>

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
                                onChange={async (e) => {
                                    const nextCourse = e.target.value;
                                    setCourseName(nextCourse);
                                    // update expected arrays for each row while preserving any entered actuals
                                    const updated = await Promise.all(
                                        flightRows.map(async row => {
                                            const expected = await getExpectedScoresForPlayer(row.player, nextCourse);
                                            return {
                                                ...row,
                                                // keep actual if present, otherwise keep nulls
                                                expected,
                                                actual: row.actual && row.actual.length === 18 ? row.actual : Array(18).fill(null)
                                            };
                                        })
                                    );
                                    setFlightRows(updated);
                                }}
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
                                    .filter(p => !flightRows.find(r => r.player.name === p.name))
                                    .map(p => (
                                        <option key={p.name} value={p.name}>{p.name}</option>
                                    ))}
                            </select>
                        </div>
                    </div>

                    <div className="overflow-x-auto w-full bg-latte-base/40 rounded-2xl border border-latte-crust shadow-inner p-4">
                        <table className="w-full text-left border-collapse min-w-[900px] table-fixed">
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
                                {/* Par row - visually emphasized */}
                                <tr className="border-b bg-latte-base border-latte-crust">
                                    <td className="p-2 text-center"></td>
                                    <td className="p-2 font-black">Par</td>
                                    {(() => {
                                        const course = courses.find((c: any) => c?.name === courseName);
                                        const pars = (course && Array.isArray(course.pars)) ? course.pars : Array.from({ length: 18 }).map(() => 3);
                                        return pars.map((p: number, i: number) => (
                                            <td key={i} className="p-0.5 text-center">
                                                <div className="w-full min-w-[32px] text-center p-1 rounded-md text-sm font-black">{p}</div>
                                            </td>
                                        ));
                                    })()}
                                    <td className="p-2 text-right font-black">{(() => {
                                        const course = courses.find((c: any) => c?.name === courseName);
                                        const pars = (course && Array.isArray(course.pars)) ? course.pars : Array.from({ length: 18 }).map(() => 3);
                                        return pars.reduce((a: number, b: number) => a + (Number(b) || 0), 0);
                                    })()}</td>
                                </tr>

                                {/* Player Actual rows only (show '-' for empty inputs) */}
                                {flightRows.map((row, rowIndex) => (
                                    <tr key={rowIndex} className="border-b border-latte-mantle hover:bg-white/30 transition-colors">
                                        <td className="p-2 text-center">
                                            <button onClick={() => removePlayerFromFlight(rowIndex)} className="text-latte-subtext hover:text-latte-red">
                                                <Trash2 size={14} />
                                            </button>
                                        </td>
                                        <td className="p-2 font-bold text-sm whitespace-nowrap">{row.player.name}</td>
                                        {row.actual.map((val, holeIndex) => (
                                            <td key={holeIndex} className="p-0.5">
                                                <input
                                                    type="number"
                                                    placeholder="-"
                                                    className="w-full min-w-[32px] text-center p-1 rounded-md border border-transparent hover:border-latte-crust text-xs font-bold focus:border-latte-mauve focus:bg-white outline-none bg-transparent"
                                                    value={val !== null && val !== undefined ? String(val) : ''}
                                                    onChange={(e) => {
                                                        const next = [...flightRows];
                                                        const parsed = e.target.value === '' ? null : parseInt(e.target.value);
                                                        next[rowIndex] = { ...next[rowIndex], actual: [...next[rowIndex].actual] };
                                                        next[rowIndex].actual[holeIndex] = parsed;
                                                        setFlightRows(next);
                                                    }}
                                                />
                                            </td>
                                        ))}
                                        <td className="p-2 text-right font-black text-latte-mauve">
                                            {row.actual.map((a, i) => (a !== null && a !== undefined ? Number(a) : Number(row.expected[i] || 0))).reduce((a, b) => a + (Number(b) || 0), 0)}
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

            {/* --- Assign modal --- */}
            {isAssignModalOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
                    <div className="bg-white rounded-2xl p-6 w-full max-w-xl">
                        <div className="flex justify-between items-center mb-4">
                            <h3 className="font-black">Assign ScoreCard to Tournament</h3>
                            <button onClick={closeAssignModal} className="text-latte-subtext font-bold">Close</button>
                        </div>
                        <div className="space-y-3">
                            {tournamentsLoading ? (
                                <div className="text-sm text-latte-subtext">Loading tournaments...</div>
                            ) : (!activeTournaments || activeTournaments.length === 0) ? (
                                <div className="text-sm text-latte-subtext">No active tournaments found.</div>
                            ) : (
                                activeTournaments.map((t: any) => (
                                    <div key={t.id} className="flex items-center justify-between p-3 border rounded-lg">
                                        <div>
                                            <div className="font-bold">{t.name}</div>
                                            <div className="text-xs text-latte-subtext">Season: {t.seasonName || t.season || ''}</div>
                                        </div>
                                        <div className="flex items-center gap-2">
                                            <button
                                                onClick={() => assignFlightToTournament(t.id)}
                                                disabled={assigning}
                                                className="px-3 py-1 text-latte-green"
                                            >
                                                <Link size={20} />
                                            </button>
                                        </div>
                                    </div>
                                ))
                            )}
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

