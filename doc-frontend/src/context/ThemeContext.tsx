import React, { createContext, useContext, useState, useEffect, type ReactNode } from 'react';

type Theme = 'light' | 'dark';

interface ThemeContextType {
  theme: Theme;
  toggleTheme: () => void;
  setTheme: (theme: Theme) => void;
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

export const useTheme = () => {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useTheme debe usarse dentro de ThemeProvider');
  }
  return context;
};

interface ThemeProviderProps {
  children: ReactNode;
}

export const ThemeProvider: React.FC<ThemeProviderProps> = ({ children }) => {
  const [theme, setThemeState] = useState<Theme>(() => {
    // Limpiar la clase dark al iniciar
    const root = window.document.documentElement;
    root.classList.remove('dark');
    
    // Obtener tema del localStorage o usar modo claro como predeterminado
    const savedTheme = localStorage.getItem('theme') as Theme;
    if (savedTheme && savedTheme === 'dark') {
      root.classList.add('dark');
      return savedTheme;
    }
    
    // Modo claro como predeterminado
    return 'light';
  });

  useEffect(() => {
    const root = window.document.documentElement;
    
    // Limpiar siempre primero para asegurar estado correcto
    root.classList.remove('dark');
    
    if (theme === 'dark') {
      root.classList.add('dark');
    }
    
    localStorage.setItem('theme', theme);
  }, [theme]);

  const toggleTheme = () => {
    setThemeState(prev => prev === 'light' ? 'dark' : 'light');
  };

  const setTheme = (newTheme: Theme) => {
    setThemeState(newTheme);
  };

  return (
    <ThemeContext.Provider value={{ theme, toggleTheme, setTheme }}>
      {children}
    </ThemeContext.Provider>
  );
};
