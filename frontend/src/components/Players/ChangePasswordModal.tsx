import { useState } from 'react';
import { X, Lock } from 'lucide-react';
import api from '../../api/client';

interface ChangePasswordModalProps {
    isOpen: boolean;
    onClose: () => void;
    playerName: string;
}

export const ChangePasswordModal = ({ isOpen, onClose, playerName }: ChangePasswordModalProps) => {
    const [password, setPassword] = useState('');

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            await api.put(`/players/${encodeURIComponent(playerName)}/password`, {
                password: password
            });
            setPassword('');
            onClose();
            alert("Password updated successfully");
        } catch (err) {
            alert("Failed to update password. Check admin permissions.");
        }
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-[100] flex items-center justify-center bg-latte-text/20 backdrop-blur-sm p-4">
            <div className="w-full max-w-sm bg-white rounded-3xl shadow-2xl p-8 border border-latte-crust">
                <div className="flex justify-between items-center mb-6 text-latte-mauve">
                    <h3 className="text-xl font-black flex items-center gap-2"><Lock /> Reset Password</h3>
                    <button onClick={onClose} className="text-latte-subtext"><X /></button>
                </div>
                <form onSubmit={handleSubmit} className="space-y-4">
                    <p className="text-xs font-bold text-latte-subtext uppercase">New Password for {playerName}</p>
                    <input
                        type="password"
                        className="w-full p-4 rounded-2xl border border-latte-crust outline-none focus:ring-2 focus:ring-latte-mauve"
                        value={password}
                        onChange={e => setPassword(e.target.value)}
                        required
                    />
                    <button type="submit" className="w-full py-4 bg-latte-mauve text-white rounded-2xl font-black shadow-lg">Update Password</button>
                </form>
            </div>
        </div>
    );
};