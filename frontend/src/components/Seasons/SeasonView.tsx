import { useState, useEffect } from 'react';
import { Plus, Calendar, Trophy, ChevronRight, Trash2, Settings2 } from 'lucide-react';
import api from '../../api/client';
import { AddTournamentModal } from './AddTournamentModal';
import { FlightManagerModal } from '../Flights/AddFlightModal';

export const SeasonsView = ({ isAdmin }: { isAdmin: boolean }) => {
    const [seasons, setSeasons] = useState<string[]>([]);
    const [selectedSeason, setSelectedSeason] = useState<string | null>(null);
    const [tournaments, setTournaments] = useState<any[]>([]);
    const [isTourneyModalOpen, setIsTourneyModalOpen] = useState(false);
    const [activeTourneyForFlights, setActiveTourneyForFlights] = useState<any | null>(null);

    useEffect(() => {
        fetchSeasons();
    }, []);

    const fetchSeasons = async () => {
        const res = await api.get('/seasons');
        setSeasons(res.data);
        if (res.data.length > 0 && !selectedSeason) setSelectedSeason(res.data[0]);
    };

    const fetchTournaments = async () => {
        if (selectedSeason) {
            const res = await api.get(`/seasons/${selectedSeason}/tournaments`);
            setTournaments(res.data);
        }
    };

    useEffect(() => {
        fetchTournaments();
    }, [selectedSeason]);

    const handleAddSeason = async () => {
        const name = window.prompt("Enter Season Name (e.g., 2026 Spring League):");
        if (name) {
            try {
                await api.post('/seasons/start', name, { headers: { 'Content-Type': 'text/plain' } });
                fetchSeasons();
            } catch (err: any) { alert(err.response?.data?.message || "Failed to create season."); }
        }
    };

    const handleDeleteSeason = async (name: string) => {
        if (window.confirm(`Are you sure you want to delete the entire "${name}" season and all its tournaments?`)) {
            try {
                await api.delete(`/seasons/${name}`);
                setSelectedSeason(null);
                fetchSeasons();
            } catch (err) { alert("Error deleting season."); }
        }
    };

    const handleDeleteTournament = async (seasonName: string, tournamentName: string) => {
        if (window.confirm(`Delete tournament "${tournamentName}"?`)) {
            try {
                // Updated path based on your new Controller mapping
                // URL: /tournament/{seasonName}/{tournamentName}
                await api.delete(`/tournaments/${seasonName}/${tournamentName}`);

                // Refresh the list after successful deletion
                fetchTournaments();
            } catch (err: any) {
                console.error("Delete failed:", err);
                alert(err.response?.data?.message || "Error deleting tournament.");
            }
        }
    };

    return (
        <div className="grid grid-cols-12 gap-8">
            {/* Sidebar: Season List */}
            <div className="col-span-12 md:col-span-4 space-y-4">
                <div className="flex justify-between items-center mb-2">
                    <h3 className="font-black text-latte-subtext uppercase tracking-widest text-xs ml-2">All Seasons</h3>
                    {isAdmin && (
                        <button onClick={handleAddSeason} className="text-latte-green hover:bg-latte-green/10 p-2 rounded-xl transition-colors">
                            <Plus size={20} />
                        </button>
                    )}
                </div>
                {seasons.map(s => (
                    <div key={s} className="group relative">
                        <button
                            onClick={() => setSelectedSeason(s)}
                            className={`w-full flex items-center justify-between p-5 rounded-2xl border transition-all ${
                                selectedSeason === s
                                    ? 'bg-white border-latte-mauve shadow-md translate-x-2'
                                    : 'bg-latte-mantle border-latte-crust hover:border-latte-subtext'
                            }`}
                        >
                            <div className="flex items-center gap-3">
                                <Calendar className={selectedSeason === s ? 'text-latte-mauve' : 'text-latte-subtext'} size={18} />
                                <span className="font-bold">{s}</span>
                            </div>
                            {selectedSeason === s && <ChevronRight size={18} className="text-latte-mauve" />}
                        </button>
                        {isAdmin && (
                            <button
                                onClick={(e) => { e.stopPropagation(); handleDeleteSeason(s); }}
                                className="absolute -right-2 -top-2 bg-latte-red text-white p-1.5 rounded-lg opacity-0 group-hover:opacity-100 transition-opacity shadow-lg"
                            >
                                <Trash2 size={14} />
                            </button>
                        )}
                    </div>
                ))}
            </div>

            {/* Main: Tournament List Area */}
            <div className="col-span-12 md:col-span-8 space-y-6">
                {selectedSeason ? (
                    <>
                        <div className="flex justify-between items-center bg-white p-8 rounded-3xl border border-latte-crust shadow-sm">
                            <div>
                                <h2 className="test-xl font-black text-latte-text">{selectedSeason}</h2>
                                <p className="text-latte-subtext font-medium">{tournaments.length} Tournaments Total</p>
                            </div>
                            {isAdmin && (
                                <button
                                    onClick={() => setIsTourneyModalOpen(true)}
                                    className="bg-latte-mauve text-white px-6 py-3 rounded-2xl font-bold flex items-center gap-2 shadow-lg hover:brightness-110 transition-all"
                                >
                                    <Trophy size={20} /> New Tournament
                                </button>
                            )}
                        </div>

                        <div className="grid grid-cols-1 gap-4">
                            {tournaments.length > 0 ? (
                                tournaments.map(t => (
                                    <div key={t.id} className="bg-white p-6 rounded-2xl border border-latte-crust flex items-center justify-between group hover:shadow-md transition-shadow">
                                        <div className="flex items-center gap-4">
                                            <div className="w-12 h-12 bg-latte-base rounded-xl flex items-center justify-center text-latte-mauve">
                                                <Trophy size={24} />
                                            </div>
                                            <div>
                                                <h4 className="font-black text-lg text-latte-text">{t.name}</h4>
                                                <p className="text-xs text-latte-subtext font-bold uppercase tracking-wider">
                                                    • {t.strategyName}
                                                </p>
                                            </div>
                                        </div>

                                        <div className="flex items-center gap-2">
                                            <button
                                                onClick={() => setActiveTourneyForFlights({ ...t, seasonName: selectedSeason })}
                                                className="..."
                                            >
                                                <Settings2 size={16} /> Manage Flights
                                            </button>
                                            {isAdmin && (
                                                <button
                                                    // 2. Pass both names to the handler
                                                    onClick={() => handleDeleteTournament(selectedSeason!, t.name)}
                                                    className="p-2 text-latte-subtext hover:text-latte-red hover:bg-latte-red/10 rounded-xl transition-all"
                                                >
                                                    <Trash2 size={20} />
                                                </button>
                                            )}
                                        </div>
                                    </div>
                                ))
                            ) : (
                                <div className="text-center py-20 bg-latte-mantle/50 rounded-3xl border-2 border-dashed border-latte-crust">
                                    <p className="text-latte-subtext italic">No tournaments found for {selectedSeason}.</p>
                                </div>
                            )}
                        </div>
                    </>
                ) : (
                    <div className="h-full flex flex-col items-center justify-center bg-white rounded-3xl border border-latte-crust p-12 text-center shadow-sm">
                        <Calendar size={40} className="text-latte-subtext mb-6" />
                        <h3 className="text-2xl font-black text-latte-text">No Season Selected</h3>
                        <p className="text-latte-subtext mt-2 max-w-xs mx-auto">
                            {seasons.length > 0 ? "Select a season to manage tournaments." : "Create your first season to get started."}
                        </p>
                    </div>
                )}
            </div>

            <AddTournamentModal
                isOpen={isTourneyModalOpen}
                onClose={() => setIsTourneyModalOpen(false)}
                seasonName={selectedSeason || ''}
                onSuccess={fetchTournaments}
            />
            <FlightManagerModal
                isOpen={!!activeTourneyForFlights}
                onClose={() => {
                    setActiveTourneyForFlights(null);
                    fetchTournaments(); // Refresh list to see if 'isFinished' changed
                }}
                tournament={activeTourneyForFlights}
            />
        </div>
    );
};