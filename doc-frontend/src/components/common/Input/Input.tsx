import React, { type InputHTMLAttributes, forwardRef } from 'react';
import './Input.css';

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  icon?: React.ReactNode;
  rightIcon?: React.ReactNode;
}

const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, icon, rightIcon, className = '', ...props }, ref) => {
    const inputClasses = [
      'input-field',
      icon ? 'has-left-icon' : '',
      rightIcon ? 'has-right-icon' : '',
      error ? 'has-error' : '',
      className
    ].filter(Boolean).join(' ');

    return (
      <div className="input-container">
        {label && (
          <label className="input-label">
            {label}
          </label>
        )}
        <div className="input-wrapper">
          {icon && (
            <div className="input-icon">
              {icon}
            </div>
          )}
          <input
            ref={ref}
            className={inputClasses}
            {...props}
          />
          {rightIcon && (
            <div className="input-right-icon">
              {rightIcon}
            </div>
          )}
        </div>
        {error && (
          <p className="input-error">{error}</p>
        )}
      </div>
    );
  }
);

Input.displayName = 'Input';

export default Input;
