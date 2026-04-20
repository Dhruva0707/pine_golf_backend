import React, {useState, useEffect} from 'react';
import {X, Plus, Info, Zap} from 'lucide-react';
import api from '../../api/client';

interface AddCourseModalProps {
    isOpen: boolean,
    onClose: () => void,
    onSuccess: () => void,
    editData?: any
}

export const AddCourseModal = ({isOpen, onClose, onSuccess, editData}: AddCourseModalProps) => {
    const [name, setName] = useState('');
    const [pars, setPars] = useState<number[]>(Array(18).fill(3));
    const [indexes, setIndexes] = useState<number[]>(Array.from({length: 18}, (_, i) => i + 1));
    const [slopeRating, setSlopeRating] = useState<number>(0);
    const [courseRating, setCourseRating] = useState<number>(0);

    // When opening the modal in edit mode, prefill the fields
    useEffect(() => {
        if (!isOpen) return;
        if (editData) {
            setName(editData.name ?? '');
            setPars(Array.isArray(editData.pars) ? editData.pars : Array(18).fill(3));
            setIndexes(Array.isArray(editData.indexes) ? editData.indexes : Array.from({length: 18}, (_, i) => i + 1));
            setSlopeRating(typeof editData.slopeRating === 'number' ? editData.slopeRating : 0);
            setCourseRating(typeof editData.courseRating === 'number' ? editData.courseRating : 0);
        } else {
            // reset for create
            setName('');
            setPars(Array(18).fill(3));
            setIndexes(Array.from({length: 18}, (_, i) => i + 1));
            setSlopeRating(0);
            setCourseRating(0);
        }
    }, [isOpen, editData]);

    if (!isOpen) return null;

    const quickSetPars = (val: number) => {
        setPars(Array(18).fill(val));
    };

    const handleSave = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!name) return alert("Course name is required");

        // Validate ratings
        if (isNaN(slopeRating) || isNaN(courseRating)) {
            return alert('Please enter valid numeric values for slope rating and course rating');
        }

        const payload = {name, pars, indexes, slopeRating, courseRating};

        try {
            if (editData) {
                // Update existing course - assume PUT /courses/{originalName}
                await api.put(`/courses/${encodeURIComponent(editData.name)}`, payload);
            } else {
                // Create new course
                await api.post('/courses', payload);
            }

            onSuccess();
            onClose();

            // reset local state after save
            setName('');
            setPars(Array(18).fill(3));
            setIndexes(Array.from({length: 18}, (_, i) => i + 1));
            setSlopeRating(0);
            setCourseRating(0);
        } catch (err) {
            console.error(err);
            alert("Failed to save course. Ensure the name is unique and your input is valid.");
        }
    };

    return (
        <div className="fixed inset-0 z-[100] flex items-center justify-center bg-latte-text/20 backdrop-blur-sm p-4">
            <div
                className="bg-white w-full max-w-4xl rounded-3xl shadow-2xl border border-latte-crust flex flex-col max-h-[95vh]">

                {/* Header */}
                <div
                    className="p-8 border-b border-latte-base flex justify-between items-center bg-white sticky top-0 rounded-t-3xl z-10">
                    <div>
                        <h3 className="text-2xl font-black flex items-center gap-2 text-latte-green">
                            <Plus size={28}/> {editData ? 'Edit Course' : 'Define New Course'}
                        </h3>
                        <p className="text-[10px] font-bold text-latte-subtext uppercase tracking-widest mt-1">Configure
                            Pars and Difficulty Indexes</p>
                    </div>
                    <button onClick={onClose} className="text-latte-subtext hover:text-latte-red transition-colors"><X/>
                    </button>
                </div>

                <form onSubmit={handleSave} className="p-8 space-y-8 overflow-y-auto">
                    {/* Course Name */}
                    <div className="space-y-2">
                        <label className="text-xs font-black uppercase text-latte-subtext ml-1">Course Name</label>
                        <input
                            required
                            className="w-full text-xl font-bold p-5 bg-latte-base/30 rounded-2xl outline-none focus:ring-2 focus:ring-latte-green border border-transparent focus:border-latte-green transition-all"
                            placeholder="e.g. Pine Woods MMR (Par 3)"
                            value={name}
                            onChange={e => setName(e.target.value)}
                        />
                    </div>

                    {/* Slope & Course Rating Row */}
                    <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-2">
                            <label className="text-xs font-black uppercase text-latte-subtext ml-1">Slope Rating</label>
                            <input
                                type="number"
                                step="0.1"
                                className="w-full text-lg font-bold p-3 bg-latte-base/30 rounded-2xl outline-none focus:ring-2 focus:ring-latte-green border border-transparent focus:border-latte-green transition-all"
                                placeholder="e.g. 113"
                                value={slopeRating}
                                onChange={e => setSlopeRating(parseFloat(e.target.value) || 0)}
                            />
                        </div>

                        <div className="space-y-2">
                            <label className="text-xs font-black uppercase text-latte-subtext ml-1">Course Rating</label>
                            <input
                                type="number"
                                step="0.1"
                                className="w-full text-lg font-bold p-3 bg-latte-base/30 rounded-2xl outline-none focus:ring-2 focus:ring-latte-green border border-transparent focus:border-latte-green transition-all"
                                placeholder="e.g. 72.4"
                                value={courseRating}
                                onChange={e => setCourseRating(parseFloat(e.target.value) || 0)}
                            />
                        </div>
                    </div>

                    {/* Quick Set Bar */}
                    <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                        <div className="flex items-center gap-2 text-latte-subtext">
                            <Info size={16}/>
                            <p className="text-xs font-bold uppercase tracking-widest">Hole Configuration</p>
                        </div>

                        <div
                            className="flex items-center gap-2 bg-latte-base/50 p-1.5 rounded-2xl border border-latte-crust">
                            <span
                                className="text-[10px] font-black uppercase text-latte-subtext px-2 flex items-center gap-1">
                                <Zap size={12} className="text-latte-yellow"/> Set all to:
                            </span>
                            {[3, 4, 5].map(v => (
                                <button
                                    key={v}
                                    type="button"
                                    onClick={() => quickSetPars(v)}
                                    className="px-4 py-1.5 rounded-xl bg-white border border-latte-crust text-xs font-black hover:border-latte-green hover:text-latte-green transition-all active:scale-95"
                                >
                                    Par {v}
                                </button>
                            ))}
                        </div>
                    </div>

                    {/* 18-Hole Grid */}
                    <div className="grid grid-cols-6 md:grid-cols-9 gap-3">
                        {pars.map((par, i) => (
                            <div key={i}
                                 className="bg-latte-base/20 p-2 rounded-xl border border-latte-crust group hover:border-latte-green/30 transition-colors">
                                <span
                                    className="text-[10px] font-black text-latte-subtext block text-center mb-2 uppercase">H{i + 1}</span>
                                <div className="space-y-1.5">
                                    <input
                                        type="number"
                                        className="w-full text-center font-bold bg-white rounded-lg py-2 text-sm outline-none focus:ring-1 focus:ring-latte-green shadow-sm"
                                        value={par}
                                        onChange={e => {
                                            const newPars = [...pars];
                                            newPars[i] = parseInt(e.target.value) || 0;
                                            setPars(newPars);
                                        }}
                                    />
                                    <input
                                        type="number"
                                        placeholder="Idx"
                                        className="w-full text-center text-[10px] font-bold text-latte-blue bg-transparent outline-none"
                                        value={indexes[i]}
                                        onChange={e => {
                                            const newIndexes = [...indexes];
                                            newIndexes[i] = parseInt(e.target.value) || 0;
                                            setIndexes(newIndexes);
                                        }}
                                    />
                                </div>
                            </div>
                        ))}
                    </div>

                    {/* Submit */}
                    <button
                        type="submit"
                        className="w-full py-5 bg-latte-green text-white rounded-2xl font-black text-lg shadow-xl hover:brightness-110 active:scale-95 transition-all uppercase tracking-widest"
                    >
                        Save Course to Library
                    </button>
                </form>
            </div>
        </div>
    );
};