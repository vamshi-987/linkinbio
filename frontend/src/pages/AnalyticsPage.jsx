import { useEffect, useState } from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, BarChart, Bar } from 'recharts';
import { getAnalyticsSummary } from '../api/analyticsApi';
import { Link } from 'react-router-dom';

export default function AnalyticsPage() {
  const [summary, setSummary] = useState(null);

  useEffect(() => { getAnalyticsSummary().then(setSummary); }, []);

  if (!summary) return <p className="text-center mt-20">Loading...</p>;

  return (
    <div className="max-w-2xl mx-auto mt-10 p-4">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Analytics</h1>
        <Link to="/dashboard" className="text-sm underline">Back to dashboard</Link>
      </div>

      <p className="mb-6">
        Total clicks (last 30 days): <span className="font-semibold">{summary.totalClicks}</span>
      </p>

      <h2 className="font-semibold mb-2">Clicks over time</h2>
      <ResponsiveContainer width="100%" height={250}>
        <LineChart data={summary.dailyClicks}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis dataKey="day" />
          <YAxis allowDecimals={false} />
          <Tooltip />
          <Line type="monotone" dataKey="clicks" stroke="#000" strokeWidth={2} />
        </LineChart>
      </ResponsiveContainer>

      <h2 className="font-semibold mt-8 mb-2">Clicks per link</h2>
      <ResponsiveContainer width="100%" height={250}>
        <BarChart data={summary.clicksPerLink}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis dataKey="title" />
          <YAxis allowDecimals={false} />
          <Tooltip />
          <Bar dataKey="clicks" fill="#000" />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}