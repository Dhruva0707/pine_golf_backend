import { useState, FormEvent } from 'react';
import { X, Trophy, Info, Zap } from 'lucide-react'; // Changed Activity to Trophy and added Zap
import api from '../../api/client';

interface AddTournamentModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSuccess: () => void;
    seasonName: string;
}

export const AddTournamentModal = ({ isOpen, onClose, seasonName, onSuccess }: AddTournamentModalProps) => {
    const [name, setName] = useState('');
    const [strategy, setStrategy] = useState('STABLEFORD');
    const [multiplier, setMultiplier] = useState(0.8);

    const [pars, setPars] = useState<number[]>(Array(18).fill(4));
    const [indexes, setIndexes] = useState<number[]>(Array.from({ length: 18 }, (_, i) => i + 1));
    const [pointsMap, setPointsMap] = useState<Record<string, number>>({ "-2": 4, "-1": 3, "0": 2, "1": 1, "2": 0 });

    if (!isOpen) return null;

    // --- NEW: Bulk Update Helpers ---
    const setAllPars = (val: number) => {
        setPars(Array(18).fill(val));
    };

    const handleParChange = (index: number, value: string) => {
        const newPars = [...pars];
        newPars[index] = parseInt(value) || 0;
        setPars(newPars);
    };

    const handlePointChange = (key: string, val: string) => {
        setPointsMap(prev => ({
            ...prev,
            [key]: parseInt(val) || 0
        }));
    };

    const handleIndexChange = (index: number, value: string) => {
        const newIndexes = [...indexes];
        newIndexes[index] = parseInt(value) || 0;
        setIndexes(newIndexes);
    };

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();
        const request = {
            name,
            season_name: seasonName,
            strategy_type: strategy,
            pars,
            indexes,
            points_map: pointsMap,
            handicap_multiplier: multiplier
        };

        try {
            await api.post('/tournaments/start', request);
            onSuccess();
            onClose();
        } catch (err) { console.error("Tourney start failed:", err); }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-latte-text/20 backdrop-blur-sm p-4 overflow-y-auto">
            <div className="w-full max-w-4xl bg-white rounded-3xl shadow-2xl border border-latte-crust p-8 my-auto max-h-[90vh] overflow-y-auto">
                <div className="flex justify-between items-center mb-6 sticky top-0 bg-white z-10 pb-4 border-b border-latte-base">
                    <h3 className="text-2xl font-black flex items-center gap-2 text-latte-mauve">
                        <Trophy size={28} /> New Tournament: {seasonName}
                    </h3>
                    <button onClick={onClose} className="text-latte-subtext hover:text-latte-red transition-colors"><X /></button>
                </div>

                <form onSubmit={handleSubmit} className="space-y-8">
                    {/* Basic Info Row */}
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                        <div className="md:col-span-1 space-y-2">
                            <label className="text-xs font-black uppercase tracking-widest text-latte-subtext">Tournament Name</label>
                            <input
                                className="w-full p-4 rounded-2xl border border-latte-crust outline-none focus:ring-2 focus:ring-latte-mauve"
                                placeholder="e.g. Pinehurst Open"
                                value={name}
                                onChange={e => setName(e.target.value)}
                                required
                            />
                        </div>
                        <div className="space-y-2">
                            <label className="text-xs font-black uppercase tracking-widest text-latte-subtext">Strategy</label>
                            <select
                                className="w-full p-4 rounded-2xl border border-latte-crust bg-white"
                                value={strategy}
                                onChange={e => setStrategy(e.target.value)}
                            >
                                <option value="STABLEFORD">Stableford</option>
                            </select>
                        </div>
                        <div className="space-y-2">
                            <label className="text-xs font-black uppercase tracking-widest text-latte-subtext flex justify-between">
                                Handicap Allowance <span>{Math.round(multiplier * 100)}%</span>
                            </label>
                            <input
                                type="range" min="0.5" max="1.5" step="0.05"
                                className="w-full h-10 accent-latte-mauve cursor-pointer"
                                value={multiplier}
                                onChange={e => setMultiplier(parseFloat(e.target.value))}
                            />
                        </div>
                    </div>

                    {/* Course Config Section */}
                    <div className="space-y-6">
                        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                            <div className="flex items-center gap-2 text-latte-subtext">
                                <Info size={16} />
                                <p className="text-xs font-bold uppercase tracking-widest">Course Layout</p>
                            </div>

                            {/* NEW: Quick Actions Bar */}
                            <div className="flex items-center gap-2 bg-latte-base/50 p-1.5 rounded-2xl border border-latte-crust">
                                <span className="text-[10px] font-black uppercase text-latte-subtext px-2 flex items-center gap-1">
                                    <Zap size={12} className="text-latte-yellow" /> Set all pars:
                                </span>
                                {[3, 4, 5].map(v => (
                                    <button
                                        key={v}
                                        type="button"
                                        onClick={() => setAllPars(v)}
                                        className="px-4 py-1.5 rounded-xl bg-white border border-latte-crust text-xs font-black hover:border-latte-mauve hover:text-latte-mauve transition-all active:scale-95"
                                    >
                                        Par {v}
                                    </button>
                                ))}
                            </div>
                        </div>

                        <div className="grid grid-cols-6 md:grid-cols-9 gap-3">
                            {pars.map((par, i) => (
                                <div key={i} className="space-y-1 bg-latte-base/30 p-2 rounded-xl border border-latte-crust hover:border-latte-mauve/30 transition-colors">
                                    <span className="text-[10px] font-black text-latte-subtext block text-center uppercase">Hole {i+1}</span>
                                    <div className="space-y-1">
                                        <input
                                            type="number"
                                            className="w-full text-center bg-white rounded-lg border border-latte-crust text-sm font-bold py-1 focus:ring-1 focus:ring-latte-mauve outline-none"
                                            value={par}
                                            onChange={(e) => handleParChange(i, e.target.value)}
                                        />
                                        <input
                                            type="number"
                                            className="w-full text-center bg-white rounded-lg border border-latte-crust text-[10px] text-latte-blue py-1 outline-none"
                                            value={indexes[i]}
                                            onChange={(e) => handleIndexChange(i, e.target.value)}
                                        />
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>

                    {/* Points Map Summary */}
                    <div className="bg-latte-mauve/5 p-6 rounded-3xl border border-latte-mauve/20">
                        <div className="flex justify-between items-center mb-4">
                            <h4 className="text-sm font-black text-latte-mauve uppercase">Point Values (Relative to Par)</h4>
                            <button
                                type="button"
                                onClick={() => setPointsMap({ "-2": 4, "-1": 3, "0": 2, "1": 1, "2": 0 })}
                                className="text-[10px] font-black uppercase text-latte-subtext hover:text-latte-mauve underline"
                            >
                                Reset to Standard
                            </button>
                        </div>

                        <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
                            {[
                                { key: "-2", label: "Eagle" },
                                { key: "-1", label: "Birdie" },
                                { key: "0",  label: "Par" },
                                { key: "1",  label: "Bogey" },
                                { key: "2",  label: "Dbl Bogey" }
                            ].map((item) => (
                                <div key={item.key} className="space-y-1">
                                    <label className="text-[10px] font-black text-latte-subtext uppercase block ml-1">
                                        {item.label}
                                    </label>
                                    <input
                                        type="number"
                                        className="w-full p-3 bg-white rounded-xl border border-latte-crust text-sm font-bold text-center focus:ring-2 focus:ring-latte-mauve outline-none"
                                        value={pointsMap[item.key]}
                                        onChange={(e) => handlePointChange(item.key, e.target.value)}
                                    />
                                </div>
                            ))}
                        </div>
                        <p className="mt-4 text-[10px] text-latte-subtext italic font-medium">
                            *Points are awarded based on net score relative to hole par.
                        </p>
                    </div>

                    <button type="submit" className="w-full py-5 bg-latte-mauve text-white rounded-2xl font-black shadow-xl hover:brightness-110 active:scale-95 transition-all text-lg">
                        Start Tournament Session
                    </button>
                </form>
            </div>
        </div>
    );
};