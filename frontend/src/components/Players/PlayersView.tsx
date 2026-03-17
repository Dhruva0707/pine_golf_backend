import { useState, useEffect } from 'react';
import { Trash2, User, Plus, Search, UserPen } from 'lucide-react';
import api from '../../api/client';
import { AddPlayerModal } from './AddPlayerModal.tsx';

interface Player {
    id: number;
    name: string;
    handicap: number;
    team?: string;
}

export const PlayersView = ({ isAdmin }: { isAdmin: boolean }) => {
    // 1. STATE MANAGEMENT
    const [players, setPlayers] = useState<Player[]>([]);
    const [search, setSearch] = useState('');
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editingPlayer, setEditingPlayer] = useState<Player | null>(null);
    const [loading, setLoading] = useState(true);

    // 2. DATA FETCHING
    const fetchPlayers = async () => {
        setLoading(true);
        try {
            const res = await api.get('/players');
            setPlayers(Array.isArray(res.data) ? res.data : []);
        } catch (err) {
            console.error("Error fetching players:", err);
        } finally {
            setLoading(false);
        }
    };

    // This hook ensures the data loads as soon as the component is "mounted"
    useEffect(() => {
        fetchPlayers();
    }, []);

    // 3. ACTION HANDLERS
    const handleDelete = async (player: Player) => {
        if (window.confirm(`Permanently delete ${player.name}?`)) {
            try {
                // Adjust this URL to match your backend (e.g., /players/1 or /players/JohnDoe)
                await api.delete(`/players/${player.name}`);
                await fetchPlayers();
            } catch (err) {
                console.error("Delete failed:", err);
                alert("Could not delete player. They may be tied to existing tournament scores.");
            }
        }
    };

    const handleEdit = (player: Player) => {
        setEditingPlayer(player);
        setIsModalOpen(true);
    };

    const handleCloseModal = () => {
        setIsModalOpen(false);
        setEditingPlayer(null); // Clear the edit data so next time it's "Add" mode
    };

    // 4. SEARCH FILTERING
    const filteredPlayers = players.filter(p =>
        p.name?.toLowerCase().includes(search.toLowerCase())
    );

    return (
        <div className="space-y-6 animate-in fade-in slide-in-from-bottom-2 duration-500">
            {/* Header Section */}
            <div className="flex justify-between items-end">
                <div>
                    <h2 className="text-3xl font-black text-latte-text tracking-tight">Roster</h2>
                    <p className="text-latte-subtext font-medium">
                        {loading ? 'Refreshing list...' : `${filteredPlayers.length} golfers found`}
                    </p>
                </div>
                {isAdmin && (
                    <button
                        onClick={() => { setEditingPlayer(null); setIsModalOpen(true); }}
                        className="bg-latte-green text-white px-6 py-3 rounded-2xl font-bold flex items-center gap-2 hover:brightness-110 hover:scale-[1.02] transition-all shadow-lg shadow-latte-green/20"
                    >
                        <Plus size={20} /> Add Player
                    </button>
                )}
            </div>

            {/* Search Input */}
            <div className="relative group">
                <Search className="absolute left-4 top-4 text-latte-subtext group-focus-within:text-latte-mauve transition-colors" size={20} />
                <input
                    className="w-full p-4 pl-12 rounded-2xl border border-latte-crust outline-none focus:ring-2 focus:ring-latte-mauve bg-white shadow-sm transition-all"
                    placeholder="Search golfers by name..."
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                />
            </div>

            {/* Data Table */}
            <div className="bg-white rounded-3xl border border-latte-crust overflow-hidden shadow-sm">
                <table className="w-full text-left border-collapse">
                    <thead className="bg-latte-mantle/50 border-b border-latte-crust">
                    <tr>
                        <th className="px-8 py-5 text-xs font-black uppercase tracking-widest text-latte-subtext">Golfer</th>
                        <th className="px-8 py-5 text-xs font-black uppercase tracking-widest text-latte-subtext">Handicap</th>
                        <th className="px-8 py-5 text-xs font-black uppercase tracking-widest text-latte-subtext">Team Status</th>
                        {isAdmin && <th className="px-8 py-5 text-xs font-black uppercase tracking-widest text-latte-subtext text-center">Actions</th>}
                    </tr>
                    </thead>
                    <tbody className="divide-y divide-latte-crust">
                    {loading ? (
                        <tr>
                            <td colSpan={4} className="px-8 py-12 text-center text-latte-subtext italic">Loading players...</td>
                        </tr>
                    ) : filteredPlayers.length > 0 ? (
                        filteredPlayers.map(p => (
                            <tr key={p.name} className="hover:bg-latte-base/30 transition-colors group">
                                <td className="px-8 py-5">
                                    <div className="flex items-center gap-3">
                                        <div className="w-8 h-8 bg-latte-base rounded-full flex items-center justify-center text-latte-subtext group-hover:bg-latte-mauve/10 group-hover:text-latte-mauve transition-colors">
                                            <User size={16} />
                                        </div>
                                        <span className="font-bold text-latte-text">{p.name}</span>
                                    </div>
                                </td>
                                <td className="px-8 py-5">
                                        <span className="bg-latte-base px-3 py-1 rounded-lg font-mono font-bold text-latte-blue">
                                            {p.handicap?.toFixed(1) ?? '0.0'}
                                        </span>
                                </td>
                                <td className="px-8 py-5 text-latte-subtext font-medium">
                                    {p.team === 'UNASSIGNED' || !p.team ? 'Independent' : p.team}
                                </td>
                                {isAdmin && (
                                    <td className="px-8 py-5">
                                        <div className="flex items-center justify-center gap-2">
                                            <button
                                                onClick={() => handleEdit(p)}
                                                className="p-2 text-latte-mauve hover:bg-latte-mauve/10 rounded-xl transition-colors"
                                                title="Edit Player"
                                            >
                                                <UserPen size={18}/>
                                            </button>
                                            <button
                                                onClick={() => handleDelete(p)}
                                                className="p-2 text-latte-red hover:bg-latte-red/10 rounded-xl transition-colors"
                                                title="Delete Player"
                                            >
                                                <Trash2 size={18}/>
                                            </button>
                                        </div>
                                    </td>
                                )}
                            </tr>
                        ))
                    ) : (
                        <tr>
                            <td colSpan={4} className="px-8 py-12 text-center text-latte-subtext italic">
                                No golfers found matching "{search}"
                            </td>
                        </tr>
                    )}
                    </tbody>
                </table>
            </div>

            {/* The Modal handles both "Add" and "Edit" internally based on if editData is passed */}
            <AddPlayerModal
                isOpen={isModalOpen}
                onClose={handleCloseModal}
                onSuccess={fetchPlayers}
                editData={editingPlayer}
            />
        </div>
    );
};