import { useState, useEffect, type FormEvent } from 'react';
import { X, LayoutDashboard } from 'lucide-react';
import api from '../../api/client';

export const AddTeamModal = ({ isOpen, onClose, onSuccess, editData }: any) => {
    const [name, setName] = useState('');

    // If editData changes (we click Edit), fill the input with the team name
    useEffect(() => {
        if (editData) setName(editData.name);
        else setName('');
    }, [editData, isOpen]);

    if (!isOpen) return null;

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();
        try {
            if (editData) {
                // EDIT MODE: Use PUT
                await api.put(`/teams/${editData.name}`, name, {
                    headers: { 'Content-Type': 'text/plain' } // Tell the server this is just text
                });
            } else {
                // ADD MODE: Use POST
                await api.post('/teams', name, {
                    headers: { 'Content-Type': 'text/plain' }
                });
            }
            onSuccess();
            onClose();
        } catch (err) { console.error(err); }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-latte-text/20 backdrop-blur-sm p-4">
            <div className="w-full max-w-sm bg-white rounded-3xl shadow-2xl p-8 border border-latte-crust">
                <div className="flex justify-between items-center mb-6">
                    <h3 className="text-xl font-black flex items-center gap-2 text-latte-text">
                        <LayoutDashboard className="text-latte-mauve" />
                        {editData ? 'Edit Team' : 'Create Team'}
                    </h3>
                    <button onClick={onClose} className="text-latte-subtext hover:text-latte-red"><X /></button>
                </div>
                <form onSubmit={handleSubmit} className="space-y-4">
                    <input
                        className="w-full p-4 rounded-2xl border border-latte-crust outline-none focus:ring-2 focus:ring-latte-mauve"
                        placeholder="Team Name"
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                        required
                    />
                    <button type="submit" className={`w-full py-4 text-white rounded-2xl font-black shadow-lg transition-transform active:scale-95 ${editData ? 'bg-latte-mauve' : 'bg-latte-green'}`}>
                        {editData ? 'Update Team' : 'Save Team'}
                    </button>
                </form>
            </div>
        </div>
    );
};