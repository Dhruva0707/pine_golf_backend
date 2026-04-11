import { useState, useEffect, useMemo } from 'react';
import { Plus, Calendar, Trophy, ChevronRight, Trash2, Settings2, CheckCircle2, Award } from 'lucide-react';
import api from '../../api/client';
import { AddTournamentModal } from './AddTournamentModal';
import { FlightManagerModal } from '../Flights/AddFlightModal';

interface TeamStandingDTO {
    teamName: string;
    points: number;
    wins: number;
    losses: number;
    draws: number;
    birdies: number;
}

export const SeasonsView = ({ isAdmin }: { isAdmin: boolean }) => {
    const [seasons, setSeasons] = useState<string[]>([]);
    const [selectedSeason, setSelectedSeason] = useState<string | null>(null);
    const [tournaments, setTournaments] = useState<any[]>([]);
    const [standings, setStandings] = useState<TeamStandingDTO[]>([]);
    const [playerMap, setPlayerMap] = useState<Record<number, string>>({}); // ID -> Name
    const [isTourneyModalOpen, setIsTourneyModalOpen] = useState(false);
    const [activeTourneyForFlights, setActiveTourneyForFlights] = useState<any | null>(null);
    const [expandedFlights, setExpandedFlights] = useState<Record<number, boolean>>({});

    const sortedSeasons = useMemo(() => {
        return [...seasons].sort((a, b) => b.localeCompare(a));
    }, [seasons]);

    const sortedStandings = useMemo(() => {
        return [...standings].sort((a, b) => b.points - a.points);
    }, [standings]);

    useEffect(() => {
        fetchSeasons();
        fetchPlayers(); // Fetch players once on mount
    }, []);

    const fetchSeasons = async () => {
        const res = await api.get('/seasons');
        setSeasons(res.data);
        if (res.data.length > 0) {
            const sorted = [...res.data].sort((a, b) => b.localeCompare(a));
            // Use functional update to avoid overriding a user selection made after this async call started
            setSelectedSeason(prev => prev ?? sorted[0]);
        }
    };

    const fetchPlayers = async () => {
        try {
            const res = await api.get('/players');
            const mapping: Record<number, string> = {};
            res.data.forEach((p: any) => {
                mapping[p.id] = p.name;
            });
            setPlayerMap(mapping);
        } catch (err) { console.error("Failed to fetch players", err); }
    };

    const PlayerName = ({ id }: { id: string | number }) => {
        const [name, setName] = useState<string>(`ID: ${id}`);

        useEffect(() => {
            const fetchName = async () => {
                try {
                    // Calling your specific /players/id/{id} endpoint
                    const res = await api.get(`/players/id/${id}`);
                    setName(res.data.name);
                } catch (err) {
                    console.error(`Failed to fetch name for player ${id}`, err);
                }
            };
            fetchName();
        }, [id]);

        return <span>{name}</span>;
    };

    const fetchDataForSeason = async () => {
        if (selectedSeason) {
            const [tourneyRes, standingsRes] = await Promise.all([
                api.get(`/seasons/${selectedSeason}/tournaments`),
                api.get(`/seasons/${selectedSeason}/standing`)
            ]);
            setTournaments(tourneyRes.data);
            setStandings(standingsRes.data);
        }
    };

    useEffect(() => {
        fetchDataForSeason();
    }, [selectedSeason]);

    // --- Handlers ---
    const handleAddSeason = async () => {
        const name = window.prompt("Enter Season Name:");
        if (name) {
            try {
                await api.post('/seasons/start', name, { headers: { 'Content-Type': 'text/plain' } });
                fetchSeasons();
            } catch (err: any) { alert(err.response?.data?.message || "Error"); }
        }
    };

    const handleDeleteSeason = async (name: string) => {
        if (window.confirm(`Delete "${name}" and all data?`)) {
            try {
                await api.delete(`/seasons/${name}`);
                setSelectedSeason(null);
                fetchSeasons();
            } catch (err) { alert("Error deleting season."); }
        }
    };

    const handleFinishSeason = async () => {
        if (window.confirm(`Finalize ${selectedSeason}?`)) {
            try {
                await api.post(`/seasons/${selectedSeason}/finish`);
                fetchDataForSeason();
            } catch (err) { alert("Error finishing season."); }
        }
    };

    const handleDeleteTournament = async (seasonName: string, tournamentName: string) => {
        if (window.confirm(`Delete tournament "${tournamentName}"?`)) {
            try {
                await api.delete(`/tournaments/${seasonName}/${tournamentName}`);
                fetchDataForSeason();
            } catch (err: any) { alert("Error deleting tournament."); }
        }
    };

    // --- Logic for Ties ---
    let currentRank = 0;
    let lastPoints = -1;

    return (
        <div className="grid grid-cols-12 gap-8">
            {/* Sidebar */}
            <div className="col-span-12 md:col-span-3 space-y-4">
                <div className="flex justify-between items-center mb-2">
                    <h3 className="font-black text-latte-subtext uppercase tracking-widest text-xs ml-2">All Seasons</h3>
                    {isAdmin && (
                        <button onClick={handleAddSeason} className="text-latte-green hover:bg-latte-green/10 p-2 rounded-xl">
                            <Plus size={20} />
                        </button>
                    )}
                </div>
                {sortedSeasons.map(s => (
                    <div key={s} className="group relative">
                        <button
                            onClick={() => setSelectedSeason(s)}
                            className={`w-full flex items-center justify-between p-4 rounded-2xl border transition-all ${
                                selectedSeason === s ? 'bg-white border-latte-mauve shadow-md' : 'bg-latte-mantle border-transparent'
                            }`}
                        >
                            <span className={`font-bold ${selectedSeason === s ? 'text-latte-text' : 'text-latte-subtext'}`}>{s}</span>
                        </button>
                        {isAdmin && (
                            <button onClick={(e) => { e.stopPropagation(); handleDeleteSeason(s); }} className="absolute -right-2 -top-2 bg-latte-red text-white p-1.5 rounded-lg opacity-0 group-hover:opacity-100 transition-opacity">
                                <Trash2 size={14} />
                            </button>
                        )}
                    </div>
                ))}
            </div>

            {/* Main */}
            <div className="col-span-12 md:col-span-9 space-y-8">
                {selectedSeason && (
                    <>
                        <div className="flex justify-between items-end">
                            <div>
                                <h2 className="text-xl font-black text-latte-text">{selectedSeason}</h2>
                                <p className="text-latte-subtext font-bold uppercase tracking-tighter">Season Dashboard</p>
                            </div>
                            <div className="flex gap-3">
                                {isAdmin && (
                                    <button onClick={handleFinishSeason} className="bg-latte-green text-white px-5 py-2.5 rounded-xl font-bold flex items-center gap-2">
                                        <CheckCircle2 size={18} /> Finish Season
                                    </button>
                                )}
                                {isAdmin && (
                                    <button onClick={() => setIsTourneyModalOpen(true)} className="bg-latte-mauve text-white px-5 py-2.5 rounded-xl font-bold flex items-center gap-2">
                                        <Trophy size={18} /> New Tournament
                                    </button>
                                )}
                            </div>
                        </div>

                        {/* Standings Table with Tie Support */}
                        <section className="bg-white rounded-3xl border border-latte-crust overflow-hidden shadow-sm">
                            <table className="w-full text-left">
                                <thead className="bg-latte-mantle text-latte-subtext text-xs uppercase font-black">
                                <tr>
                                    <th className="px-6 py-3">Rank</th>
                                    <th className="px-6 py-3">Team</th>
                                    <th className="px-6 py-3 text-center">Pts</th>
                                    <th className="px-6 py-3 text-center">W-L-D</th>
                                </tr>
                                </thead>
                                <tbody className="divide-y divide-latte-crust">
                                {(() => {
                                    // Reset trackers for the render pass
                                    let currentRank = 0;
                                    let lastPoints = -1;

                                    return sortedStandings.map((team, idx) => {
                                        // If points are different from previous, update rank to the current position (1-indexed)
                                        // If points are the same, they keep the same rank (Tied)
                                        if (team.points !== lastPoints) {
                                            currentRank = idx + 1;
                                            lastPoints = team.points;
                                        }

                                        return (
                                            <tr key={team.teamName} className="hover:bg-latte-base/10 transition-colors">
                                                <td className="px-6 py-4 font-black text-latte-subtext">
                                                    {/* Added a '#' prefix for visual clarity */}
                                                    #{currentRank}
                                                </td>
                                                <td className="px-6 py-4 font-bold text-latte-text">
                                                    {team.teamName}
                                                </td>
                                                <td className="px-6 py-4 text-center font-black text-latte-mauve">
                                                    {team.points}
                                                </td>
                                                <td className="px-6 py-4 text-center text-latte-subtext text-sm">
                                                    {team.wins}-{team.losses}-{team.draws}
                                                </td>
                                            </tr>
                                        );
                                    });
                                })()}
                                </tbody>
                            </table>
                        </section>

                        {/* Tournaments with Player Name Awards */}
                        <div className="space-y-4">
                            {tournaments.map(t => (
                                <div key={t.id} className="bg-white rounded-2xl border border-latte-crust overflow-hidden shadow-sm">
                                    {t.awards && Object.keys(t.awards).length > 0 && (
                                        <div className="bg-latte-yellow/10 p-4 border-b border-latte-yellow/20 flex gap-4 overflow-x-auto">
                                            <div className="flex items-center gap-2 text-latte-yellow-dark font-black text-xs uppercase whitespace-nowrap">
                                                <Award size={16} /> Awards:
                                            </div>
                                            {Object.entries(t.awards)
                                                .sort((a, b) => (a[1] as number) - (b[1] as number))
                                                .map(([playerId, rank]) => (
                                                    <div
                                                        key={playerId}
                                                        className="bg-white px-3 py-1 rounded-full border border-latte-yellow/30 text-xs font-bold shadow-sm flex items-center gap-1"
                                                    >
                                                        <PlayerName id={playerId} />, Rank {String(rank)}
                                                    </div>
                                                ))
                                            }
                                        </div>
                                    )}

                                    <div className="p-5 flex items-center justify-between">
                                        <div className="flex items-center gap-4">
                                            <div className="w-10 h-10 bg-latte-mantle rounded-xl flex items-center justify-center text-latte-mauve">
                                                <Trophy size={20} />
                                            </div>
                                            <div>
                                                <h4 className="font-black text-latte-text">{t.name}</h4>
                                                <p className="text-[10px] text-latte-subtext font-bold uppercase tracking-widest">{t.strategyName}</p>
                                            </div>
                                        </div>
                                        <div className="flex gap-2">
                                            <button onClick={() => setExpandedFlights(prev => ({ ...prev, [t.id]: !prev[t.id] }))} className="px-4 py-2 text-sm font-bold bg-latte-base rounded-xl hover:bg-latte-crust flex items-center gap-2">
                                                <ChevronRight size={16} className={`${expandedFlights[t.id] ? 'rotate-90' : ''} transition-transform`} /> View Flights
                                            </button>
                                            <button onClick={() => setActiveTourneyForFlights({...t, seasonName: selectedSeason})} className="px-4 py-2 text-sm font-bold bg-latte-base rounded-xl hover:bg-latte-crust flex items-center gap-2">
                                                <Settings2 size={16} /> Manage
                                            </button>
                                            {isAdmin && <button onClick={() => handleDeleteTournament(selectedSeason!, t.name)} className="p-2 text-latte-subtext hover:text-latte-red rounded-lg"><Trash2 size={18} /></button>}
                                        </div>
                                    </div>

                                    {expandedFlights[t.id] && (
                                        <div className="px-5 pb-5">
                                            {(!t.flights || t.flights.length === 0) ? (
                                                <div className="text-sm text-latte-subtext font-bold bg-latte-mantle rounded-xl p-4">No flights recorded yet.</div>
                                            ) : (
                                                <div className="space-y-4">
                                                    {t.flights.map((fd: any, fIdx: number) => (
                                                        <div key={fIdx} className="border border-latte-crust rounded-xl overflow-hidden">
                                                            <div className="bg-latte-base px-4 py-2 flex items-center gap-2 text-latte-subtext text-xs uppercase font-black">
                                                                <Calendar size={14} /> Flight on {fd.date ? new Date(fd.date).toLocaleString() : 'Unknown date'}
                                                            </div>
                                                            <div className="p-4">
                                                                <div className="grid grid-cols-12 text-xs font-black text-latte-subtext uppercase mb-2">
                                                                    <div className="col-span-6">Player</div>
                                                                    <div className="col-span-3 text-center">Score</div>
                                                                    <div className="col-span-3 text-center">Birdies</div>
                                                                </div>
                                                                <div className="divide-y divide-latte-crust">
                                                                    {fd.flights && fd.flights.map((fs: any, sIdx: number) => (
                                                                        <div key={sIdx} className="grid grid-cols-12 py-2 items-center">
                                                                            <div className="col-span-6 font-bold text-latte-text">{fs.playerName}</div>
                                                                            <div className="col-span-3 text-center font-black text-latte-mauve">{fs.score}</div>
                                                                            <div className="col-span-3 text-center text-latte-subtext">{fs.birdies}</div>
                                                                        </div>
                                                                    ))}
                                                                </div>
                                                            </div>
                                                        </div>
                                                    ))}
                                                </div>
                                            )}
                                        </div>
                                    )}
                                </div>
                            ))}
                        </div>
                    </>
                )}
            </div>

            <AddTournamentModal isOpen={isTourneyModalOpen} onClose={() => setIsTourneyModalOpen(false)} seasonName={selectedSeason || ''} onSuccess={fetchDataForSeason} />
            <FlightManagerModal isOpen={!!activeTourneyForFlights} onClose={() => { setActiveTourneyForFlights(null); fetchDataForSeason(); }} tournament={activeTourneyForFlights} />
        </div>
    );
};