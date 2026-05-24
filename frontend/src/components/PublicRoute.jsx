import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function PublicRoute({ children }) {
  const { isAuthenticated, loading } = useAuth();

  if (loading) {
    return (
      <div className="auth-container">
        <p style={{ color: 'var(--text-secondary)' }}>Učitavanje...</p>
      </div>
    );
  }

  if (isAuthenticated()) {
    return <Navigate to="/dashboard" replace />;
  }

  return children;
}
