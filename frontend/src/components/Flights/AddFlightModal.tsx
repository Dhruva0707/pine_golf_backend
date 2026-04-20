import { useState, useEffect } from 'react';
import { X, Trophy, Award, Trash2 } from 'lucide-react';
import api from '../../api/client';

export interface FlightManagerModalProps {
    isOpen: boolean;
    onClose: () => void;
    tournament: any;
}

export const FlightManagerModal = ({ isOpen, onClose, tournament }: FlightManagerModalProps) => {
    // --- State ---
    const [players, setPlayers] = useState<any[]>([]);
    // flightRows now holds expected and actual arrays; actual uses null for un-entered values
    const [flightRows, setFlightRows] = useState<{ player: any; expected: number[]; actual: (number | null)[] }[]>([]);
    const [selectedPlayerId, setSelectedPlayerId] = useState('');
    const [isFinished, setIsFinished] = useState(false);

    const getLocalDefaultScores = () => {
        const pars = tournament?.course?.pars ?? tournament?.pars ?? [];
        return Array.from({ length: 18 }, (_, i) => Number(pars[i]) || 3);
    };

    const getDefaultScoresForPlayer = async (player: any) => {
        const tournamentId = tournament?.id;
        const handicap = player.handicap;
        const multiplier = tournament?.handicapMultiplier ?? 1.0; // use tournament multiplier if present
        console.log("fetching handicap scores for ", player.name, " (", tournamentId, ", ", handicap, ") with multiplier", multiplier)
        if (tournamentId !== undefined && handicap !== undefined) {
            try {
                // Backend endpoint that supports handicapMultiplier
                const res = await api.get(`/tournaments/${tournamentId}/${handicap}/handicapScore?multiplier=${multiplier}`);
                if (Array.isArray(res.data) && res.data.length > 0) {
                    return res.data.map((v: any, i: number) => Number(v) || getLocalDefaultScores()[i] || 3);
                }
            } catch (err) {
                console.error('Failed to fetch handicap-based expected scores', err);
            }
        }
        return getLocalDefaultScores();
    };

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
            setPlayers(pRes.data ?? []);
            if (tRes.data.flights) setFlightRows([]);
        } catch (err) {
            console.error("Failed to load flight data", err);
        }
    };

    // --- Logic ---
    const addPlayerToFlight = async (playerName: string) => {
        if (!playerName) return;
        const playerObj = players.find(p => p.name === playerName);
        if (playerObj) {
            const expected = await getDefaultScoresForPlayer(playerObj);
            setFlightRows(prev => [...prev, { player: playerObj, expected, actual: Array(18).fill(null) }]);
            setSelectedPlayerId('');
        }
    };

    const removePlayerFromFlight = (index: number) => {
        setFlightRows(prev => prev.filter((_, i) => i !== index));
    };

    const handleAddFlight = async () => {
        if (flightRows.length === 0) return;

        // Build payload: fill missing actuals with expected
        const payload = flightRows.map(row => ({
            player: row.player,
            holeScores: row.actual.map((a, idx) => (a !== null && a !== undefined ? a : row.expected[idx]))
        }));

        try {
            await api.post(`/tournaments/${tournament.id}/flights`, payload);
            setFlightRows([]);
            onClose();
        } catch (err) {
            alert("Error adding flight. Ensure the tournament is active.");
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
                        <button onClick={onClose} className="text-latte-subtext hover:text-latte-red"><X /></button>
                    </div>
                </div>

                <div className="flex-1 overflow-hidden flex flex-col">
                    <div className="flex-1 overflow-y-auto p-6">
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
                                            {/* Par row - emphasized */}
                                            <tr className="border-b border-latte-mauve bg-latte-mauve/10">
                                                <td className="p-2 text-center"></td>
                                                <td className="p-2 font-black text-latte-mauve">Par</td>
                                                {(() => {
                                                    const pars = tournament?.course?.pars ?? tournament?.pars ?? Array.from({ length: 18 }).map(() => 3);
                                                    return pars.map((p: number, i: number) => (
                                                        <td key={i} className="p-0.5 text-center"><div className="text-sm font-black text-latte-mauve">{p}</div></td>
                                                    ));
                                                })()}
                                                <td className="p-2 text-right font-black text-latte-mauve">{(() => {
                                                    const pars = tournament?.course?.pars ?? tournament?.pars ?? Array.from({ length: 18 }).map(() => 3);
                                                    return pars.reduce((a: number, b: number) => a + (Number(b) || 0), 0);
                                                })()}</td>
                                            </tr>

                                            {/* Player Actual rows only */}
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
                                                    <td className="p-2 text-right font-black text-latte-mauve">{row.actual.map((a, i) => (a !== null && a !== undefined ? Number(a) : Number(row.expected[i] || 0))).reduce((a, b) => a + (Number(b) || 0), 0)}</td>
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
                    </div>
                </div>
            </div>
        </div>
    );
};