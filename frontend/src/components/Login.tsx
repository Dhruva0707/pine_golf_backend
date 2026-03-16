import React, { useState } from 'react';
import api from '../api/client';
import { Trophy } from 'lucide-react';

export const Login = ({ onLoginSuccess }: { onLoginSuccess: () => void }) => {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');

    const handleLogin = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            const response = await api.post('/auth/login', { username, password });
            // Depending on your backend, this might be response.data.token or just response.data
            const token = typeof response.data === 'string' ? response.data : response.data.token;

            localStorage.setItem('golf_token', token);
            onLoginSuccess();
        } catch (err) {
            setError('Invalid username or password');
        }
    };

    return (
        <div className="flex min-h-screen items-center justify-center bg-latte-base px-4">
            <form onSubmit={handleLogin} className="w-full max-w-md bg-white p-10 rounded-2xl shadow-xl border border-latte-crust">
                <div className="flex flex-col items-center mb-8">
                    <div className="w-16 h-16 bg-latte-green text-white rounded-full flex items-center justify-center mb-4 shadow-lg">
                        <Trophy size={32} />
                    </div>
                    <h1 className="text-3xl font-bold text-latte-text">Fairway Login</h1>
                    <p className="text-latte-subtext mt-2">Enter your credentials to tee off</p>
                </div>

                {error && (
                    <div className="bg-latte-red/10 border border-latte-red/20 text-latte-red px-4 py-2 rounded-lg mb-4 text-sm text-center">
                        {error}
                    </div>
                )}

                <div className="space-y-4">
                    <div>
                        <label className="block text-sm font-semibold text-latte-subtext mb-1">Username</label>
                        <input
                            type="text"
                            className="w-full p-3 rounded-lg border border-latte-crust bg-latte-base focus:outline-none focus:ring-2 focus:ring-latte-mauve"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            required
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-semibold text-latte-subtext mb-1">Password</label>
                        <input
                            type="password"
                            className="w-full p-3 rounded-lg border border-latte-crust bg-latte-base focus:outline-none focus:ring-2 focus:ring-latte-mauve"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                        />
                    </div>
                    <button
                        type="submit"
                        className="w-full py-3 bg-latte-mauve text-white font-bold rounded-lg shadow-md hover:scale-[1.02] transition-transform active:scale-95 mt-4"
                    >
                        Login
                    </button>
                </div>
            </form>
        </div>
    );
};