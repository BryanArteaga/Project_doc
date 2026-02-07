import React from 'react';
import './Loading.css';

interface LoadingProps {
  message?: string;
  size?: 'sm' | 'md' | 'lg';
  fullScreen?: boolean;
}

const Loading: React.FC<LoadingProps> = ({ message, size = 'md', fullScreen = false }) => {
  const spinnerClass = `loading-spinner loading-spinner-${size}`;
  
  const content = (
    <div className="loading-container">
      <div className={spinnerClass}></div>
      {message && (
        <p className="loading-message">{message}</p>
      )}
    </div>
  );

  if (fullScreen) {
    return (
      <div className="loading-fullscreen">
        {content}
      </div>
    );
  }

  return content;
};

export default Loading;
