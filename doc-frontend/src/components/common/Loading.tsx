import React from 'react';
import './Loading.css';

interface LoadingProps {
  message?: string;
  size?: 'sm' | 'md' | 'lg' | 'xl';
  fullScreen?: boolean;
  fullHeight?: boolean;
}

const Loading: React.FC<LoadingProps> = ({ 
  message = 'Cargando...', 
  size = 'md',
  fullScreen = false,
  fullHeight = false
}) => {
  const content = (
    <div className="loading-content">
      <div 
        className={`loading-spinner ${size}`}
        role="status"
        aria-label="Cargando"
      ></div>
      {message && (
        <p className="loading-message">{message}</p>
      )}
    </div>
  );

  if (fullScreen) {
    return (
      <div className="loading-full-screen">
        {content}
      </div>
    );
  }

  if (fullHeight) {
    return (
      <div className="loading-container full-height">
        {content}
      </div>
    );
  }

  return (
    <div className="loading-container">
      {content}
    </div>
  );
};

export default Loading;

