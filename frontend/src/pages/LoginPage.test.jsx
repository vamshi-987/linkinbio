import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { BrowserRouter } from 'react-router-dom';
import { AuthProvider } from '../context/AuthContext';
import LoginPage from './LoginPage';

describe('LoginPage', () => {
  it('renders username and password fields', () => {
    render(
      <BrowserRouter>
        <AuthProvider><LoginPage /></AuthProvider>
      </BrowserRouter>
    );
    expect(screen.getByPlaceholderText('username')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('password')).toBeInTheDocument();
  });
});