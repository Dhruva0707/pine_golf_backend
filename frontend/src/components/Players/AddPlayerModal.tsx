import { useState, useEffect, type FormEvent } from 'react';
import { X, UserPlus } from 'lucide-react';
import api from '../../api/client.ts';

interface AddPlayerModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSuccess: () => void;
}

export const AddPlayerModal = ({ isOpen, onClose, onSuccess }: AddPlayerModalProps) => {
    const [name, setName] = useState('');
    const [handicap, setHandicap] = useState('0.0');
    const [team, setTeam] = useState('');
    const [teams, setTeams] = useState<any[]>([]);

    useEffect(() => {
        if (isOpen) {
            api.get('/teams').then(res => setTeams(res.data)).catch(console.error);
        }
    }, [isOpen]);

    if (!isOpen) return null;

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();
        try {
            await api.post('/players', {
                name,
                handicap: parseFloat(handicap),
                team: team === '' ? null : team // Optional team
            });
            setName(''); setHandicap('0.0'); setTeam('');
            onSuccess();
            onClose();
        } catch (err) { console.error(err); }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-latte-text/20 backdrop-blur-sm p-4">
            <div className="w-full max-w-md bg-white rounded-3xl shadow-2xl border border-latte-crust p-8">
                <div className="flex justify-between items-center mb-6 text-latte-green">
                    <h3 className="text-xl font-black flex items-center gap-2"><UserPlus /> Add Player</h3>
                    <button onClick={onClose} className="text-latte-subtext hover:text-latte-red"><X /></button>
                </div>
                <form onSubmit={handleSubmit} className="space-y-4">
                    <input className="w-full p-4 rounded-2xl border border-latte-crust" placeholder="Player Name" value={name} onChange={e => setName(e.target.value)} required />
                    <input className="w-full p-4 rounded-2xl border border-latte-crust" type="number" step="0.1" value={handicap} onChange={e => setHandicap(e.target.value)} required />

                    <select
                        className="w-full p-4 rounded-2xl border border-latte-crust bg-white"
                        value={team}
                        onChange={e => setTeam(e.target.value)}
                    >
                        <option value="">No Team (Optional)</option>
                        {teams.map(t => <option key={t.id} value={t.id}>{t.name}</option>)}
                    </select>

                    <button type="submit" className="w-full py-4 bg-latte-green text-white rounded-2xl font-black shadow-lg">Save Player</button>
                </form>
            </div>
        </div>
    );
};