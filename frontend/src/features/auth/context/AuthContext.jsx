import { createContext, useContext, useState, useEffect } from 'react';
import api from '@/shared/services/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Proveri da li postoji sačuvan korisnik u localStorage
    const savedUser = localStorage.getItem('user');
    const savedToken = localStorage.getItem('token');
    if (savedUser && savedToken) {
      setUser(JSON.parse(savedUser));
    }
    setLoading(false);
  }, []);

  const login = async (email, lozinka) => {
    const response = await api.post('/auth/login', { email, lozinka });
    const data = response.data;
    localStorage.setItem('token', data.token);
    const userData = {
      korisnikId: data.korisnikId,
      ime: data.ime,
      prezime: data.prezime,
      email: data.email,
      tipKorisnika: data.tipKorisnika,
      uloga: data.uloga,
      tokenType: data.tokenType,
    };
    localStorage.setItem('user', JSON.stringify(userData));
    setUser(userData);
    return userData;
  };

  const register = async (formData) => {
    const response = await api.post('/auth/register', formData);
    const data = response.data;
    localStorage.setItem('token', data.token);
    const userData = {
      korisnikId: data.korisnikId,
      ime: data.ime,
      prezime: data.prezime,
      email: data.email,
      tipKorisnika: data.tipKorisnika,
      uloga: data.uloga,
      tokenType: data.tokenType,
    };
    localStorage.setItem('user', JSON.stringify(userData));
    setUser(userData);
    return userData;
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setUser(null);
  };

  const isAuthenticated = () => !!user && !!localStorage.getItem('token');

  const hasRole = (role) => {
    if (!user) return false;
    return user.tipKorisnika === role || user.uloga === role;
  };

  const getUserId = () => user?.korisnikId || null;

  const getUserRole = () => user?.uloga || null;

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, isAuthenticated, hasRole, getUserId, getUserRole }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth mora biti unutar AuthProvider-a');
  return context;
};
