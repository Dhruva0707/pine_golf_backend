import { useState, useEffect } from 'react';
import { Plus, LayoutDashboard, Trash2, Pencil } from 'lucide-react';
import api from '../../api/client';
import { AddTeamModal } from './AddTeamModal';

export const TeamsView = ({ isAdmin }: { isAdmin: boolean }) => {
    const [teams, setTeams] = useState<any[]>([]);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editingTeam, setEditingTeam] = useState<any>(null); // Track which team we are editing

    const fetchTeams = async () => {
        try {
            const res = await api.get('/teams');
            setTeams(res.data);
        } catch (err) { console.error(err); }
    };

    useEffect(() => { fetchTeams(); }, []);

    const handleDelete = async (id: number, name: string) => {
        if (window.confirm(`Delete team "${name}"? This cannot be undone.`)) {
            try {
                await api.delete(`/teams/${name}`);
                await fetchTeams();
            } catch (err) { alert("Could not delete team. Is it currently assigned to players?"); }
        }
    };

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center">
                <h2 className="text-3xl font-black">Teams</h2>
                {isAdmin && (
                    <button onClick={() => { setEditingTeam(null); setIsModalOpen(true); }}
                            className="bg-latte-green text-white px-6 py-3 rounded-2xl font-bold flex items-center gap-2 shadow-lg">
                        <Plus size={20} /> Add Team
                    </button>
                )}
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {teams.map(team => (
                    <div key={team.id} className="bg-white p-6 rounded-3xl border border-latte-crust shadow-sm flex items-center justify-between group">
                        <div className="flex items-center gap-4">
                            <div className="w-12 h-12 bg-latte-mauve/10 text-latte-mauve rounded-2xl flex items-center justify-center">
                                <LayoutDashboard size={24} />
                            </div>
                            <div>
                                <h3 className="font-bold text-lg">{team.name}</h3>
                                <p className="text-latte-subtext text-sm">Official Squad</p>
                            </div>
                        </div>

                        {isAdmin && (
                            <div className="flex gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                                <button
                                    onClick={() => { setEditingTeam(team); setIsModalOpen(true); }}
                                    className="p-2 hover:bg-latte-base rounded-xl text-latte-subtext hover:text-latte-mauve"
                                >
                                    <Pencil size={18} />
                                </button>
                                <button
                                    onClick={() => handleDelete(team.id, team.name)}
                                    className="p-2 hover:bg-latte-red/10 rounded-xl text-latte-subtext hover:text-latte-red"
                                >
                                    <Trash2 size={18} />
                                </button>
                            </div>
                        )}
                    </div>
                ))}
            </div>

            <AddTeamModal
                isOpen={isModalOpen}
                onClose={() => { setIsModalOpen(false); setEditingTeam(null); }}
                onSuccess={fetchTeams}
                editData={editingTeam} // Pass the team we want to edit
            />
        </div>
    );
};