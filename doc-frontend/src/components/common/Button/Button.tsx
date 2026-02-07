import React, { type ButtonHTMLAttributes } from 'react';
import './Button.css';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'danger' | 'success' | 'outline';
  size?: 'sm' | 'md' | 'lg';
  loading?: boolean;
  fullWidth?: boolean;
  children: React.ReactNode;
}

const Button: React.FC<ButtonProps> = ({
  variant = 'primary',
  size = 'md',
  loading = false,
  fullWidth = false,
  children,
  className = '',
  disabled,
  style,
  ...props
}) => {
  const classes = [
    'button',
    `button-${variant}`,
    `button-${size}`,
    fullWidth ? 'button-full-width' : '',
    className
  ].filter(Boolean).join(' ');

  // Estilos inline como fallback para Tailwind reset
  const inlineStyles: React.CSSProperties = {
    backgroundColor: variant === 'primary' ? '#5B65DC' : 
                     variant === 'secondary' ? '#5B65DC' :
                     variant === 'danger' ? '#dc2626' :
                     variant === 'success' ? '#16a34a' :
                     variant === 'outline' ? '#5B65DC' : '#5B65DC',
    color: '#ffffff',
    border: 'none',
    padding: size === 'sm' ? '0.375rem 0.75rem' :
             size === 'lg' ? '0.75rem 1.5rem' :
             '0.5rem 1rem',
    borderRadius: '0.5rem',
    fontWeight: 600,
    cursor: disabled || loading ? 'not-allowed' : 'pointer',
    opacity: disabled || loading ? 0.5 : 1,
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: size === 'sm' ? '0.875rem' :
              size === 'lg' ? '1.125rem' : '1rem',
    ...style
  };

  return (
    <button
      className={classes}
      style={inlineStyles}
      disabled={disabled || loading}
      {...props}
    >
      {loading && (
        <svg
          className="button-loading-spinner"
          xmlns="http://www.w3.org/2000/svg"
          fill="none"
          viewBox="0 0 24 24"
        >
          <circle
            className="opacity-25"
            cx="12"
            cy="12"
            r="10"
            stroke="currentColor"
            strokeWidth="4"
          />
          <path
            className="opacity-75"
            fill="currentColor"
            d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
          />
        </svg>
      )}
      {children}
    </button>
  );
};

export default Button;
