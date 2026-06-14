import { Navigate } from 'react-router-dom';
import { useAuth } from '@/features/auth/context/AuthContext';

export default function ProtectedRoute({ children }) {
  const { isAuthenticated, loading } = useAuth();

  if (loading) {
    return (
      <div className="auth-container">
        <p style={{ color: 'var(--text-secondary)' }}>Učitavanje...</p>
      </div>
    );
  }

  if (!isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }

  return children;
}
