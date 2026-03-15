import { useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';

console.log('🔍 [AUDIT] usePageTransition.ts CARGADO');

export const usePageTransition = () => {
  const [isTransitioning, setIsTransitioning] = useState(false);
  const location = useLocation();

  useEffect(() => {
    setIsTransitioning(true);
    const timer = setTimeout(() => {
      setIsTransitioning(false);
    }, 300);

    return () => clearTimeout(timer);
  }, [location.pathname]);

  return isTransitioning;
};
