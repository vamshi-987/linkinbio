import axiosClient from './axiosClient';

export const verifyEmail = (email, otp) =>
  axiosClient.post('/auth/verify-email', { email, otp }).then((r) => r.data);

export const resendVerification = (email) =>
  axiosClient.post('/auth/resend-verification', { email }).then((r) => r.data);

export const forgotPassword = (username) =>
  axiosClient.post('/auth/forgot-password', { username }).then((r) => r.data);

export const verifyOtp = (username, otp) =>
  axiosClient.post('/auth/verify-otp', { username, otp }).then((r) => r.data);

export const resetPassword = (resetToken, newPassword) =>
  axiosClient.post('/auth/reset-password', { resetToken, newPassword }).then((r) => r.data);
