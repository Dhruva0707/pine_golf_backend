import { useState, useEffect } from 'react';
import { Login } from './components/Login';
import { Dashboard } from './components/Dashboard';

function App() {
    const [isLoggedIn, setIsLoggedIn] = useState(false);

    useEffect(() => {
        // Check if a token already exists when the page loads
        const token = localStorage.getItem('golf_token');
        if (token) setIsLoggedIn(true);
    }, []);

    if (!isLoggedIn) {
        return <Login onLoginSuccess={() => setIsLoggedIn(true)} />;
    }

    return <Dashboard />;
}

export default App;