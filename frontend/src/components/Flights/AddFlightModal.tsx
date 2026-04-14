import { useState, useEffect } from 'react';
import { X, Trophy, Award, Trash2 } from 'lucide-react';
import api from '../../api/client';
import confetti from 'canvas-confetti';

export interface FlightManagerModalProps {
    isOpen: boolean;
    onClose: () => void;
    tournament: any;
}

export const FlightManagerModal = ({ isOpen, onClose, tournament }: FlightManagerModalProps) => {
    // --- State ---
    const [players, setPlayers] = useState<any[]>([]);
    const [flights, setFlights] = useState<any[]>([]);
    const [leaderboard, setLeaderboard] = useState<any[] | null>(null);
    const [activeTab, setActiveTab] = useState<'entry' | 'leaderboard'>('entry');
    const [flightRows, setFlightRows] = useState<{player: any, scores: number[]}[]>([]);
    const [selectedPlayerId, setSelectedPlayerId] = useState('');
    const [isFinished, setIsFinished] = useState(false);

    // --- Effects ---
    useEffect(() => {
        if (isOpen && tournament) {
            loadInitialData();
            setIsFinished(tournament.isFinished);
        }
    }, [isOpen, tournament]);

    const loadInitialData = async () => {
        try {
            const [pRes, tRes] = await Promise.all([
                api.get('/players'),
                api.get(`/tournaments/${tournament.seasonName}/${tournament.name}`)
            ]);
            setPlayers(pRes.data);
            if (tRes.data.flights) setFlights(tRes.data.flights);
            // Try live leaderboard for active tournaments; fallback to flights if it fails
            if (!tournament.isFinished) {
                try {
                    const lbRes = await api.get(`/tournaments/${tournament.seasonName}/${tournament.name}/leaderBoard`);
                    setLeaderboard(lbRes.data || []);
                } catch (e) {
                    // Fallback: keep leaderboard null so UI uses flights flattening
                    setLeaderboard(null);
                }
            } else {
                setLeaderboard(null);
            }
        } catch (err) {
            console.error("Failed to load flight data", err);
            // On total failure, ensure leaderboard falls back
            setLeaderboard(null);
        }
    };

    // --- Logic ---
    const addPlayerToFlight = (playerName: string) => {
        if (!playerName) return;
        const playerObj = players.find(p => p.name === playerName);
        if (playerObj) {
            // Defaulting to 3 as requested for a light start
            setFlightRows([...flightRows, { player: playerObj, scores: Array(18).fill(3) }]);
            setSelectedPlayerId('');
        }
    };

    const removePlayerFromFlight = (index: number) => {
        setFlightRows(prev => prev.filter((_, i) => i !== index));
    };

    const handleAddFlight = async () => {
        if (flightRows.length === 0) return;

        const payload = flightRows.map(row => ({
            player: row.player,
            holeScores: row.scores
        }));

        try {
            const res = await api.post(`/tournaments/${tournament.id}/flights`, payload);
            setFlights(prev => [...prev, res.data]);
            setFlightRows([]);
            setActiveTab('leaderboard');
            // Refresh live leaderboard if available
            if (!tournament.isFinished) {
                try {
                    const lbRes = await api.get(`/tournaments/${tournament.seasonName}/${tournament.name}/leaderBoard`);
                    setLeaderboard(lbRes.data || []);
                } catch (e) {
                    // If it fails, fallback remains flights-based
                    setLeaderboard(null);
                }
            }
        } catch (err) {
            alert("Error adding flight. Ensure the tournament is active.");
        }
    };

    const handleEndTournament = async () => {
        if (window.confirm("Finalize tournament? This calculates winners and updates standings.")) {
            try {
                await api.post(`/tournaments/${tournament.id}/end`);
                setIsFinished(true);
                confetti({
                    particleCount: 150,
                    spread: 70,
                    origin: { y: 0.6 }
                });
            } catch (err) {
                alert("Failed to end tournament.");
            }
        }
    };

    if (!isOpen || !tournament) return null;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-latte-text/40 backdrop-blur-sm p-4">
            <div className="w-full max-w-6xl bg-latte-base rounded-3xl shadow-2xl flex flex-col max-h-[90vh] overflow-hidden border border-latte-crust">

                {/* Modal Header */}
                <div className="p-6 bg-white border-b border-latte-crust flex justify-between items-center">
                    <div>
                        <h3 className="text-2xl font-black text-latte-text flex items-center gap-2">
                            <Trophy className="text-latte-mauve" /> {tournament.name}
                        </h3>
                        <p className="text-xs font-bold text-latte-subtext uppercase tracking-widest">Flight & Score Management</p>
                    </div>
                    <div className="flex items-center gap-4">
                        {!isFinished && (
                            <button onClick={handleEndTournament} className="bg-latte-red text-white px-4 py-2 rounded-xl font-bold text-sm shadow-md hover:brightness-110">
                                End Tournament
                            </button>
                        )}
                        <button onClick={onClose} className="text-latte-subtext hover:text-latte-red"><X /></button>
                    </div>
                </div>

                <div className="flex-1 overflow-hidden flex flex-col">
                    {/* Tab Switcher */}
                    <div className="flex bg-latte-mantle p-1 mx-6 mt-4 rounded-xl w-fit border border-latte-crust">
                        <button
                            onClick={() => setActiveTab('entry')}
                            className={`px-6 py-2 rounded-lg font-black text-xs uppercase tracking-widest transition-all ${activeTab === 'entry' ? 'bg-white text-latte-mauve shadow-sm' : 'text-latte-subtext'}`}
                        >
                            Score Entry
                        </button>
                        <button
                            onClick={() => setActiveTab('leaderboard')}
                            className={`px-6 py-2 rounded-lg font-black text-xs uppercase tracking-widest transition-all ${activeTab === 'leaderboard' ? 'bg-white text-latte-mauve shadow-sm' : 'text-latte-subtext'}`}
                        >
                            Live Leaderboard
                        </button>
                    </div>

                    <div className="flex-1 overflow-y-auto p-6">
                        {activeTab === 'entry' ? (
                            <div className="space-y-6 animate-in fade-in duration-300">
                                {!isFinished ? (
                                    <>
                                        <div className="overflow-x-auto w-full bg-white rounded-2xl border border-latte-crust shadow-inner p-4">
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
                                                    <tr key={rowIndex} className="border-b border-latte-mantle hover:bg-latte-base/10 transition-colors">
                                                        <td className="p-2 text-center">
                                                            <button onClick={() => removePlayerFromFlight(rowIndex)} className="text-latte-subtext hover:text-latte-red">
                                                                <Trash2 size={14} />
                                                            </button>
                                                        </td>
                                                        <td className="p-2 font-bold text-sm whitespace-nowrap">{row.player.name}</td>
                                                        {row.scores.map((score, holeIndex) => (
                                                            <td key={holeIndex} className="p-0.5">
                                                                <input
                                                                    type="number"
                                                                    className="w-full min-w-[32px] text-center p-1 rounded-md border border-transparent hover:border-latte-crust text-xs font-bold focus:border-latte-mauve focus:bg-white outline-none bg-transparent"
                                                                    value={score || ''}
                                                                    onChange={(e) => {
                                                                        const newRows = [...flightRows];
                                                                        newRows[rowIndex].scores[holeIndex] = parseInt(e.target.value) || 0;
                                                                        setFlightRows(newRows);
                                                                    }}
                                                                />
                                                            </td>
                                                        ))}
                                                        <td className="p-2 text-right font-black text-latte-mauve">
                                                            {row.scores.reduce((a, b) => a + b, 0)}
                                                        </td>
                                                    </tr>
                                                ))}
                                                </tbody>
                                            </table>

                                            <div className="mt-4 p-1 bg-latte-mantle/50 rounded-xl border border-dashed border-latte-subtext/20">
                                                <select
                                                    className="w-full bg-transparent font-bold outline-none text-xs p-3 cursor-pointer text-latte-subtext"
                                                    value={selectedPlayerId}
                                                    onChange={(e) => addPlayerToFlight(e.target.value)}
                                                >
                                                    <option value="">+ Add Player to this Flight...</option>
                                                    {players
                                                        .filter(p => !flightRows.find(r => r.player.name === p.name))
                                                        .map(p => <option key={p.name} value={p.name}>{p.name}</option>)
                                                    }
                                                </select>
                                            </div>
                                        </div>

                                        <button
                                            onClick={handleAddFlight}
                                            disabled={flightRows.length === 0}
                                            className="w-full py-4 bg-latte-mauve text-white rounded-2xl font-black shadow-lg hover:brightness-110 transition-all disabled:opacity-50 uppercase tracking-widest text-sm"
                                        >
                                            Submit Flight
                                        </button>
                                    </>
                                ) : (
                                    <div className="h-full flex flex-col items-center justify-center text-center p-10">
                                        <Award size={60} className="text-latte-yellow mb-4" />
                                        <h3 className="text-xl font-black">Tournament Finalized</h3>
                                        <p className="text-latte-subtext text-sm">Standings are now official.</p>
                                    </div>
                                )}
                            </div>
                        ) : (
                            <div className="max-w-2xl mx-auto space-y-3 animate-in slide-in-from-bottom-4 duration-300">
                                {(leaderboard ?? flights.flatMap(f => f.flights || []))
                                    .sort((a, b) => b.score - a.score)
                                    .map((fs, idx) => (
                                        <div key={idx} className="bg-white p-4 rounded-2xl border border-latte-crust flex justify-between items-center shadow-sm">
                                            <div className="flex items-center gap-3">
                                                <span className="text-xs font-black text-latte-subtext w-4">#{idx+1}</span>
                                                <span className="font-bold">{fs.playerName}</span>
                                            </div>
                                            <div className="flex items-center gap-4">
                                                <span className="text-[10px] font-black text-latte-blue uppercase">Birdies: {fs.birdies}</span>
                                                <span className="bg-latte-mauve text-white px-3 py-1 rounded-lg font-black">{fs.score}</span>
                                            </div>
                                        </div>
                                    ))}
                                {leaderboard === null && flights.length === 0 && (
                                    <div className="text-center py-10 text-latte-subtext italic">No scores recorded yet.</div>
                                )}
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};