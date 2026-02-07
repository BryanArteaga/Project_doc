import React from 'react';

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
  const sizeClasses = {
    sm: 'h-6 w-6',
    md: 'h-12 w-12',
    lg: 'h-16 w-16',
    xl: 'h-24 w-24'
  };

  const content = (
    <div className="flex flex-col items-center justify-center gap-4">
      <div 
        className={`animate-spin rounded-full border-b-2 border-brand-main dark:border-brand-main ${sizeClasses[size]}`}
        role="status"
        aria-label="Cargando"
      ></div>
      {message && (
        <p className="text-brand-dark dark:text-gray-400 text-sm font-medium">{message}</p>
      )}
    </div>
  );

  if (fullScreen) {
    return (
      <div className="fixed inset-0 bg-brand-lighter dark:bg-gray-900 bg-opacity-95 dark:bg-opacity-95 flex items-center justify-center z-50 transition-colors">
        {content}
      </div>
    );
  }

  if (fullHeight) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center">
        {content}
      </div>
    );
  }

  return (
    <div className="flex items-center justify-center py-12">
      {content}
    </div>
  );
};

export default Loading;

