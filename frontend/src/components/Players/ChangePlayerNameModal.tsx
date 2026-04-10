import { useState, useEffect } from 'react';
import { X, UserRoundPen } from 'lucide-react';
import api from '../../api/client';

interface ChangeNameProps {
    isOpen: boolean;
    onClose: () => void;
    currentName: string;
    onSuccess: (newName: string) => void;
}

export const ChangePlayerNameModal = ({ isOpen, onClose, currentName, onSuccess }: ChangeNameProps) => {
    const [newName, setNewName] = useState(currentName);

    useEffect(() => { setNewName(currentName); }, [currentName]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            // Sends the newName inside the DTO object
            await api.put(`/players/${encodeURIComponent(currentName)}`, {
                newName: newName
            });
            onSuccess(newName);
            onClose();
        } catch (err) {
            alert("Could not change name. It might already be taken.");
        }
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-[100] flex items-center justify-center bg-latte-text/20 backdrop-blur-sm p-4">
            <div className="w-full max-w-sm bg-white rounded-3xl shadow-2xl p-8 border border-latte-crust">
                <div className="flex justify-between items-center mb-6 text-latte-blue">
                    <h3 className="text-xl font-black flex items-center gap-2"><UserRoundPen /> Rename Player</h3>
                    <button onClick={onClose} className="text-latte-subtext"><X /></button>
                </div>
                <form onSubmit={handleSubmit} className="space-y-4">
                    <div className="space-y-1">
                        <p className="text-[10px] font-bold text-latte-subtext uppercase ml-1">Current: {currentName}</p>
                        <input
                            type="text"
                            className="w-full p-4 rounded-2xl border border-latte-crust outline-none focus:ring-2 focus:ring-latte-blue"
                            value={newName}
                            onChange={e => setNewName(e.target.value)}
                            required
                        />
                    </div>
                    <button type="submit" className="w-full py-4 bg-latte-blue text-white rounded-2xl font-black shadow-lg">
                        Update Identity
                    </button>
                </form>
            </div>
        </div>
    );
};