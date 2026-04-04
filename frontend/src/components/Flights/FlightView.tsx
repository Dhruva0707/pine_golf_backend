import { useState, useEffect } from 'react';
import { Send, History, Plus, Calendar, Target, User, Search, X } from 'lucide-react';
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

export const FlightsView = () => {
    const [view, setView] = useState<'history' | 'add'>('history');
    const [flights, setFlights] = useState<FlightDTO[]>([]);
    const [searchPlayer, setSearchPlayer] = useState('');
    const [loading, setLoading] = useState(false);
    const [players, setPlayers] = useState<string[]>([]);

    // Form State for New Flight
    const [courseName, setCourseName] = useState('');
    const [targetPlayer, setTargetPlayer] = useState('');
    const [holes, setHoles] = useState<number[]>(new Array(18).fill(0));

    // Fetch Logic
    const fetchFlights = async (playerName?: string) => {
        setLoading(true);
        try {
            // If playerName exists, hit the specific player endpoint, else hit global flights
            const url = playerName
                ? `/players/${playerName}/flights`
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

    useEffect(() => {
        // Initial load (global)
        fetchFlights();
        fetchPlayers();
    }, []);

    const handleSearch = (e: React.FormEvent) => {
        e.preventDefault();
        fetchFlights(searchPlayer);
    };

    const clearSearch = () => {
        setSearchPlayer('');
        fetchFlights();
    };

    const handleSubmitFlight = async () => {
        if (!courseName || !targetPlayer) return alert("Course and Player Name are required");

        const totalScore = holes.reduce((a, b) => a + (Number(b) || 0), 0);
        const payload: FlightScoreDTO[] = [{
            playerName: targetPlayer,
            courseName,
            holeScores: holes,
            score: totalScore,
            birdies: 0
        }];

        try {
            await api.post('/flights', payload);
            alert("Flight saved!");
            setCourseName('');
            setTargetPlayer('');
            setHoles(new Array(18).fill(0));
            setView('history');
            fetchFlights();
        } catch (err) { alert("Error saving flight."); }
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
                            const entries = searchPlayer ? allEntries.filter(fs => fs.playerName === searchPlayer) : allEntries;
                            if (entries.length === 0) return null;

                            const course = entries[0]?.courseName || allEntries[0]?.courseName || '';

                            return (
                                <div key={i} className="bg-white rounded-2xl border border-latte-crust shadow-sm overflow-hidden hover:border-latte-mauve transition-colors">
                                    {/* Top header with Course on the left and Date on the right */}
                                    <div className="bg-latte-mantle/50 px-6 py-3 flex justify-between items-center border-b border-latte-crust">
                                        <div className="flex items-center gap-3">
                                            <div className="bg-white p-2 rounded-lg shadow-sm text-latte-mauve">
                                                <Target size={16} />
                                            </div>
                                            <span className="font-black text-latte-text">{course}</span>
                                        </div>
                                        <span className="text-[10px] font-black text-latte-subtext uppercase flex items-center gap-1">
                                            <Calendar size={12} /> {new Date(f.date).toLocaleDateString()}
                                        </span>
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
                <div className="bg-white p-8 rounded-3xl border border-latte-crust shadow-sm max-w-3xl mx-auto">
                    <div className="grid grid-cols-2 gap-4 mb-8">
                        <div>
                            <label className="block text-xs font-black uppercase text-latte-subtext mb-2 ml-1">Player Name</label>
                            <input
                                value={targetPlayer}
                                onChange={(e) => setTargetPlayer(e.target.value)}
                                className="w-full bg-latte-mantle border-transparent rounded-xl px-4 py-3 font-bold focus:ring-2 ring-latte-mauve outline-none transition-all"
                                placeholder="e.g. Tiger Woods"
                            />
                        </div>
                        <div>
                            <label className="block text-xs font-black uppercase text-latte-subtext mb-2 ml-1">Course</label>
                            <input
                                value={courseName}
                                onChange={(e) => setCourseName(e.target.value)}
                                className="w-full bg-latte-mantle border-transparent rounded-xl px-4 py-3 font-bold focus:ring-2 ring-latte-mauve outline-none transition-all"
                                placeholder="Pebble Beach"
                            />
                        </div>
                    </div>

                    <div className="mb-8">
                        <p className="text-xs font-black uppercase text-latte-subtext mb-4 ml-1">Scorecard (Holes 1-18)</p>
                        <div className="grid grid-cols-6 md:grid-cols-9 gap-3">
                            {holes.map((score, i) => (
                                <div key={i}>
                                    <label className="block text-[10px] font-black text-center text-latte-subtext mb-1">{i + 1}</label>
                                    <input
                                        type="number"
                                        min="1"
                                        value={score || ''}
                                        onChange={(e) => {
                                            const newHoles = [...holes];
                                            newHoles[i] = parseInt(e.target.value) || 0;
                                            setHoles(newHoles);
                                        }}
                                        className="w-full bg-latte-mantle text-center border-transparent rounded-xl py-3 font-black focus:bg-white focus:ring-2 ring-latte-mauve outline-none transition-all"
                                    />
                                </div>
                            ))}
                        </div>
                    </div>

                    <button
                        onClick={handleSubmitFlight}
                        className="w-full bg-latte-mauve text-white py-4 rounded-2xl font-black uppercase tracking-widest flex items-center justify-center gap-3 hover:shadow-lg hover:opacity-95 transition-all"
                    >
                        <Send size={20} /> Save Scorecard
                    </button>
                </div>
            )}
        </div>
    );
};