import { useState, useEffect } from 'react';
import {LayoutDashboard, Users, Calendar, LogOut, Send, Map} from 'lucide-react';
import { jwtDecode } from 'jwt-decode';

// Import our specialized views
import { PlayersView } from './Players/PlayersView';
import { TeamsView } from './Teams/TeamsView';
import {SeasonsView} from "./Seasons/SeasonView";
import {CourseManager} from "./Courses/CourseView";
import {FlightsView} from "./Flights/FlightView";

interface DecodedToken {
    roles: { authority: string }[];
    sub: string;
}

export const Dashboard = () => {
    // 1. STATE: This is the "brain" of the component.
    // default to the ScoreCards (Flights) view when arriving from login
    const [activeTab, setActiveTab] = useState('scoreCards');
    const [isAdmin, setIsAdmin] = useState(false);
    const [currentUserName, setCurrentUserName] = useState<string | null>(null);

    // 2. EFFECT: This runs once when the page loads.
    useEffect(() => {
        const token = localStorage.getItem('golf_token');
        if (token) {
            try {
                const decoded = jwtDecode<DecodedToken>(token);
                // Check if the user has the 'ROLE_ADMIN' authority
                const hasAdminRole = decoded.roles?.some(r => r.authority === 'ROLE_ADMIN') ?? false;
                setIsAdmin(hasAdminRole);
                setCurrentUserName(decoded.sub);
            } catch (e) {
                console.error("Token decoding failed", e);
            }
        }
    }, []);

    const logout = () => {
        localStorage.removeItem('golf_token');
        window.location.reload(); // Refreshing the app clears the state and triggers App.tsx logic
    };

    return (
        <div className="min-h-screen flex flex-col bg-latte-base text-latte-text font-sans">
            {/* --- NAVIGATION HEADER --- */}
            <header className="bg-white border-b border-latte-crust px-4 py-3 md:px-6 md:py-4 flex items-center justify-between shadow-sm sticky top-0 z-10">
                <div className="flex items-center gap-3">
                    {/* We removed bg-latte-green and padding from the img. p-1 adds a small, clean gap. */}
                    <div className="w-10 h-10 rounded-full flex items-center justify-center overflow-hidden shadow-md">
                        <img
                            src="/logo.png"
                            alt="Pinewoods logo"
                            className="w-full h-full object-cover p-1"
                        />
                    </div>
                    <div className="flex flex-col">
                        <span className="font-black text-lg leading-tight uppercase tracking-tight">Fairway</span>
                        <span className="text-[10px] font-bold text-latte-subtext uppercase tracking-[0.2em]">Manager</span>
                    </div>
                </div>

                <button
                    onClick={logout}
                    className="flex items-center gap-2 text-latte-red font-bold text-sm hover:bg-latte-red/10 px-4 py-2 rounded-xl transition-all"
                >
                    <LogOut size={18} /> Logout
                </button>
            </header>

            {/* Tab Switchers - moved below the main header so it's on its own row */}
            {/* Center the tabs within their row and align with main content padding */}
            <div className="max-w-7xl mx-auto w-full px-6 md:px-10">
                <nav className="mt-4 flex items-center justify-start bg-latte-mantle p-1 rounded-xl border border-latte-crust overflow-x-auto no-scrollbar whitespace-nowrap max-w-full gap-1">
                    {[
                        { id: 'seasons', label: 'Seasons', icon: <Calendar size={16}/> },
                        { id: 'teams', label: 'Teams', icon: <LayoutDashboard size={16}/> },
                        { id: 'players', label: 'Players', icon: <Users size={16}/> },
                        { id: 'courses', label: 'Courses', icon: <Map size={16}/> },
                        { id: 'scoreCards', label: 'ScoreCards', icon: <Send size={16}/> }
                    ].map((tab) => (
                        <button
                            key={tab.id}
                            onClick={() => setActiveTab(tab.id)} // 3. EVENT HANDLER: Updates the state
                            className={`flex items-center gap-2 px-5 py-2 rounded-lg font-bold text-sm transition-all ${
                                activeTab === tab.id
                                    ? 'bg-latte-mauve text-white shadow-md'
                                    : 'text-latte-subtext hover:text-latte-text hover:bg-latte-base'
                            }`}
                        >
                            {tab.icon} {tab.label}
                        </button>
                    ))}
                </nav>
             </div>

            {/* --- CONTENT AREA --- */}
            <main className="flex-1 p-6 md:p-10 max-w-7xl mx-auto w-full">
                {/* 4. CONDITIONAL RENDERING: Shows a component based on the activeTab */}
                {activeTab === 'players' && <PlayersView isAdmin={isAdmin} currentUserName={currentUserName} />}
                {activeTab === 'teams' && <TeamsView isAdmin={isAdmin} />}
                {activeTab === 'seasons' && <SeasonsView isAdmin={isAdmin} />}
                {activeTab === 'courses' && <CourseManager isAdmin={isAdmin} />}
                {activeTab === 'scoreCards' && <FlightsView />}
            </main>
        </div>
    );
};

