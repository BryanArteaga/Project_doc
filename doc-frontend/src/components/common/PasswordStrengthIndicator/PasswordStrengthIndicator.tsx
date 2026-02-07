import React from 'react';
import { getPasswordStrength } from '../../../utils/passwordStrength';
import './PasswordStrengthIndicator.css';

interface PasswordStrengthIndicatorProps {
  password: string;
  showSuggestions?: boolean;
}

const PasswordStrengthIndicator: React.FC<PasswordStrengthIndicatorProps> = ({ 
  password, 
  showSuggestions = true 
}) => {
  if (!password) return null;

  const strength = getPasswordStrength(password);
  const widthPercentage = strength.score;

  return (
    <div className="password-strength-container">
      <div className="password-strength-bar-container">
        <div
          className="password-strength-bar"
          style={{
            width: `${widthPercentage}%`,
            backgroundColor: strength.color,
            transition: 'all 0.3s ease'
          }}
        />
      </div>
      <div className="password-strength-info">
        <span className="password-strength-label" style={{ color: strength.color }}>
          {strength.message}
        </span>
        <span className="password-strength-score">
          {strength.score}%
        </span>
      </div>
      {showSuggestions && strength.suggestions.length > 0 && (
        <div className="password-strength-suggestions">
          <p className="suggestions-title">Sugerencias:</p>
          <ul className="suggestions-list">
            {strength.suggestions.map((suggestion, index) => (
              <li key={index}>{suggestion}</li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
};

export default PasswordStrengthIndicator;
