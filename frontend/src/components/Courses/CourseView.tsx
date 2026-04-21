import React, { useState, useEffect } from 'react';
import { Plus, Trash2, Map as MapIcon, MapPin, ChevronDown, ChevronUp, Edit3 } from 'lucide-react';
import api from '../../api/client';
import { AddCourseModal } from './AddCourseModal';

interface CourseManagerProps {
    isAdmin: boolean;
}

export const CourseManager = ({ isAdmin }: CourseManagerProps) => {
    const [courses, setCourses] = useState<any[]>([]);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editingCourse, setEditingCourse] = useState<any | null>(null);
    const [expandedCourse, setExpandedCourse] = useState<string | null>(null);

    useEffect(() => {
        loadCourses();
    }, []);

    const loadCourses = async () => {
        try {
            const res = await api.get('/courses');
            setCourses(Array.isArray(res.data) ? res.data : []);
        } catch (err) {
            console.error("Failed to load courses", err);
        }
    };

    const handleEdit = (course: any) => {
        setEditingCourse(course);
        setIsModalOpen(true);
    };

    const deleteCourse = async (name: string) => {
        if (window.confirm(`Are you sure you want to delete "${name}"? This cannot be undone.`)) {
            try {
                await api.delete(`/courses/${name}`);
                loadCourses();
            } catch (err) {
                alert("Failed to delete course. It might be in use by a tournament.");
            }
        }
    };

    return (
        <div className="space-y-8 animate-in fade-in duration-500">
            {/* --- HEADER SECTION (RESTORED) --- */}
            <div className="flex justify-between items-end">
                <div>
                    <h2 className="text-3xl font-black flex items-center gap-3 text-latte-text">
                        <MapIcon className="text-latte-green" size={32} /> Course Library
                    </h2>
                    <p className="text-latte-subtext font-bold uppercase text-[10px] tracking-widest mt-1 ml-1">
                        Physical venues and hole configurations
                    </p>
                </div>
                {isAdmin && (
                    <button
                        onClick={() => {
                            setEditingCourse(null); // Ensure we aren't in edit mode
                            setIsModalOpen(true);
                        }}
                        className="bg-latte-green text-white px-6 py-3 rounded-2xl font-black flex items-center gap-2 shadow-lg hover:brightness-110 active:scale-95 transition-all"
                    >
                        <Plus size={20} /> Add New Course
                    </button>
                )}
            </div>

            {/* --- COURSE LIST --- */}
            {courses.length === 0 ? (
                <div className="bg-white rounded-3xl p-16 border border-latte-crust text-center border-dashed">
                    <MapPin size={48} className="mx-auto text-latte-mantle mb-4" />
                    <h3 className="text-xl font-bold text-latte-subtext">No courses found</h3>
                    <p className="text-sm text-latte-subtext/60">Add your first golf course to start tracking tournaments.</p>
                </div>
            ) : (
                <div className="grid grid-cols-1 gap-4">
                    {courses.map(course => (
                        <div key={course.name} className="bg-white rounded-3xl border border-latte-crust shadow-sm overflow-hidden transition-all hover:border-latte-mauve/30">
                            {/* Card Main Row */}
                            <div className="p-5 flex items-center justify-between">
                                <div className="flex items-center gap-4">
                                    <div className="w-12 h-12 bg-latte-base rounded-2xl flex items-center justify-center text-latte-green">
                                        <MapIcon size={24} />
                                    </div>
                                    <div>
                                        <h3 className="text-xl font-black text-latte-text leading-tight">{course.name}</h3>
                                        <div className="flex gap-2 mt-1">
                                            <span className="text-[10px] font-black uppercase text-latte-subtext bg-latte-base px-2 py-0.5 rounded">
                                                Par {course.pars.reduce((a: number, b: number) => a + b, 0)}
                                            </span>
                                            <span className="text-[10px] font-black uppercase text-latte-subtext bg-latte-base px-2 py-0.5 rounded">
                                                18 Holes
                                            </span>
                                        </div>
                                    </div>
                                </div>

                                <div className="flex items-center gap-2">
                                    {/* Only show expand/collapse to admins */}
                                    {isAdmin && (
                                        <>
                                            <button
                                                onClick={() => setExpandedCourse(expandedCourse === course.name ? null : course.name)}
                                                className="p-2 text-latte-subtext hover:bg-latte-base rounded-xl transition-all"
                                                title="View Details"
                                            >
                                                {expandedCourse === course.name ? <ChevronUp size={20}/> : <ChevronDown size={20}/>}
                                            </button>
                                            <>
                                                <button
                                                    onClick={() => handleEdit(course)}
                                                    className="p-2 text-latte-mauve hover:bg-latte-mauve/10 rounded-xl transition-all"
                                                    title="Edit Course"
                                                >
                                                    <Edit3 size={18} />
                                                </button>
                                                <button
                                                    onClick={() => deleteCourse(course.name)}
                                                    className="p-2 text-latte-red hover:bg-latte-red/10 rounded-xl transition-all"
                                                    title="Delete Course"
                                                >
                                                    <Trash2 size={18} />
                                                </button>
                                            </>
                                        </>
                                    )}
                                </div>
                            </div>

                            {/* --- EXPANDABLE HOLE DATA GRID --- */}
                            {isAdmin && expandedCourse === course.name && (
                                <div className="px-6 pb-6 pt-2 border-t border-latte-base bg-latte-mantle/30 animate-in slide-in-from-top-2">
                                    <div className="grid grid-cols-6 md:grid-cols-9 lg:grid-cols-18 gap-2 mt-4">
                                        {course.pars.map((par: number, i: number) => (
                                            <div key={i} className="flex flex-col items-center">
                                                <span className="text-[9px] font-bold text-latte-subtext mb-1 italic">Hole {i + 1}</span>
                                                <div className="w-full bg-white border border-latte-crust rounded-xl p-2 text-center shadow-sm">
                                                    <div className="text-sm font-black text-latte-text">{par}</div>
                                                    <div className="text-[8px] font-bold text-latte-blue border-t border-latte-base mt-1 pt-1">
                                                        Idx {course.indexes[i]}
                                                    </div>
                                                </div>
                                            </div>
                                        ))}

                                        {/* Also show explicit slope and course rating in expanded area for clarity */}
                                        <div className="col-span-6 md:col-span-9 lg:col-span-18 mt-4 flex gap-4">
                                            <div className="bg-white p-4 rounded-2xl border border-latte-crust w-full">
                                                <div className="text-xs font-black text-latte-subtext uppercase">Slope Rating</div>
                                                <div className="text-2xl font-black text-latte-text">{typeof course.slopeRating === 'number' ? course.slopeRating : '—'}</div>
                                            </div>
                                            <div className="bg-white p-4 rounded-2xl border border-latte-crust w-full">
                                                <div className="text-xs font-black text-latte-subtext uppercase">Course Rating</div>
                                                <div className="text-2xl font-black text-latte-text">{typeof course.courseRating === 'number' ? course.courseRating : '—'}</div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            )}
                        </div>
                    ))}
                </div>
            )}

            <AddCourseModal
                isOpen={isModalOpen}
                onClose={() => {
                    setIsModalOpen(false);
                    setEditingCourse(null);
                }}
                onSuccess={loadCourses}
                editData={editingCourse}
            />
        </div>
    );
};

