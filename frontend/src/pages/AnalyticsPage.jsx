import { useEffect, useMemo, useState } from 'react';
import {
  Area, Bar, BarChart, CartesianGrid, ComposedChart, LabelList, Legend,
  Line, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts';
import { Link } from 'react-router-dom';
import { getAnalyticsSummary } from '../api/analyticsApi';

const BLUE = '#6ba6f5';
const TREND = '#8fa3bd';
const SURFACE = '#23262d';
const GRID = '#2f333c';
const AXIS_TEXT = '#9aa0ab';

const nf = new Intl.NumberFormat();

const formatDay = (iso) => {
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(y, m - 1, d).toLocaleDateString(undefined, { month: 'short', day: '2-digit' });
};

/** Least-squares fit over the daily series, so the dotted line is a real trend, not a smoothing. */
function withTrend(dailyClicks) {
  const n = dailyClicks.length;
  if (n === 0) return [];

  const sumX = (n * (n - 1)) / 2;
  const sumY = dailyClicks.reduce((acc, d) => acc + d.clicks, 0);
  const sumXY = dailyClicks.reduce((acc, d, i) => acc + i * d.clicks, 0);
  const sumXX = dailyClicks.reduce((acc, _, i) => acc + i * i, 0);

  const denominator = n * sumXX - sumX * sumX;
  const slope = denominator === 0 ? 0 : (n * sumXY - sumX * sumY) / denominator;
  const intercept = (sumY - slope * sumX) / n;

  return dailyClicks.map((d, i) => ({
    ...d,
    label: formatDay(d.day),
    trend: Math.max(0, intercept + slope * i),
  }));
}

const truncate = (text, max = 15) => (text.length > max ? `${text.slice(0, max - 1)}…` : text);

function ChartTooltip({ active, payload, label }) {
  if (!active || !payload?.length) return null;
  return (
    <div className="an-tooltip">
      <p className="an-tooltip-label">{label}</p>
      {payload.map((entry) => (
        <p key={entry.dataKey} className="an-tooltip-row">
          <span className="an-tooltip-dot" style={{ background: entry.color }} />
          {entry.name}: <strong>{nf.format(Math.round(entry.value))}</strong>
        </p>
      ))}
    </div>
  );
}

function TableView({ caption, columns, rows }) {
  return (
    <details className="an-table-toggle">
      <summary>View as table</summary>
      <div className="an-table-scroll">
        <table className="an-table">
          <caption className="an-visually-hidden">{caption}</caption>
          <thead>
            <tr>{columns.map((c) => <th key={c} scope="col">{c}</th>)}</tr>
          </thead>
          <tbody>
            {rows.map(([name, clicks]) => (
              <tr key={name}>
                <th scope="row">{name}</th>
                <td>{nf.format(clicks)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </details>
  );
}

export default function AnalyticsPage() {
  const [summary, setSummary] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    getAnalyticsSummary()
      .then(setSummary)
      .catch(() => setError('Could not load your analytics. Please try again.'));
  }, []);

  const daily = useMemo(() => withTrend(summary?.dailyClicks ?? []), [summary]);
  const perLink = useMemo(
    () => (summary?.clicksPerLink ?? []).map((l) => ({ ...l, label: truncate(l.title) })),
    [summary],
  );

  const header = (
    <>
      <Link to="/dashboard" className="an-back">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
             strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
          <path d="M15 18 9 12l6-6" />
        </svg>
        Back
      </Link>
      <h1 className="an-title">Link Analytics Overview</h1>
    </>
  );

  if (error) {
    return (
      <div className="an-screen">
        <style>{styles}</style>
        <div className="an-container">
          {header}
          <p className="an-message an-message-error" role="alert">{error}</p>
        </div>
      </div>
    );
  }

  if (!summary) {
    return (
      <div className="an-screen">
        <style>{styles}</style>
        <div className="an-container">
          {header}
          <p className="an-message" role="status">Loading your analytics…</p>
        </div>
      </div>
    );
  }

  const hasClicks = summary.totalClicks > 0;
  // Give every bar a fixed slot rather than letting a few bars stretch to fill a fixed height,
  // so bar thickness stays constant whether the user has 2 links or 12.
  const barChartHeight = Math.max(260, perLink.length * 46 + 60);

  return (
    <div className="an-screen">
      <style>{styles}</style>
      <div className="an-container">
        {header}

        <section className="an-hero" aria-label="Total clicks">
          <p className="an-hero-value">Total Clicks: {nf.format(summary.totalClicks)}</p>
          <p className="an-hero-sub">(Last {summary.windowDays} Days)</p>
        </section>

        {!hasClicks && (
          <p className="an-message">
            No clicks recorded in the last {summary.windowDays} days yet. Share your page and check back.
          </p>
        )}

        <div className="an-grid">
          <section className="an-card">
            <h2 className="an-card-title">Clicks Over Time</h2>
            <ResponsiveContainer width="100%" height={300}>
              <ComposedChart data={daily} margin={{ top: 8, right: 16, bottom: 0, left: -8 }}>
                <defs>
                  <linearGradient id="an-fill" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor={BLUE} stopOpacity={0.28} />
                    <stop offset="100%" stopColor={BLUE} stopOpacity={0.02} />
                  </linearGradient>
                </defs>
                <CartesianGrid stroke={GRID} vertical={false} />
                <XAxis
                  dataKey="label"
                  tick={{ fill: AXIS_TEXT, fontSize: 12 }}
                  tickLine={false}
                  axisLine={{ stroke: GRID }}
                  minTickGap={24}
                />
                <YAxis
                  tick={{ fill: AXIS_TEXT, fontSize: 12 }}
                  tickLine={false}
                  axisLine={false}
                  allowDecimals={false}
                  width={56}
                  tickFormatter={(v) => nf.format(v)}
                />
                <Tooltip content={<ChartTooltip />} cursor={{ stroke: GRID, strokeWidth: 1 }} />
                <Legend
                  verticalAlign="top"
                  height={30}
                  iconType="plainline"
                  wrapperStyle={{ fontSize: 13, color: AXIS_TEXT }}
                />
                <Area
                  type="monotone" dataKey="clicks" legendType="none" tooltipType="none"
                  stroke="none" fill="url(#an-fill)" isAnimationActive={false}
                />
l
              </ComposedChart>
            </ResponsiveContainer>
            <TableView
              caption="Clicks over time"
              columns={['Day', 'Clicks']}
              rows={daily.map((d) => [d.label, d.clicks])}
            />
          </section>

          <section className="an-card">
            <h2 className="an-card-title">Clicks Per Link</h2>
            {perLink.length === 0 ? (
              <p className="an-message">Add a link to start tracking clicks.</p>
            ) : (
              <>
                <ResponsiveContainer width="100%" height={barChartHeight}>
                  <BarChart
                    data={perLink}
                    layout="vertical"
                    margin={{ top: 8, right: 48, bottom: 0, left: 8 }}
                  >
                    <CartesianGrid stroke={GRID} horizontal={false} />
                    <XAxis
                      type="number"
                      tick={{ fill: AXIS_TEXT, fontSize: 12 }}
                      tickLine={false}
                      axisLine={{ stroke: GRID }}
                      allowDecimals={false}
                      tickFormatter={(v) => nf.format(v)}
                    />
                    <YAxis
                      type="category"
                      dataKey="label"
                      tick={{ fill: AXIS_TEXT, fontSize: 12 }}
                      tickLine={false}
                      axisLine={false}
                      width={110}
                    />
                    <Tooltip content={<ChartTooltip />} cursor={{ fill: 'rgba(255,255,255,0.04)' }} />
                    <Bar
                      dataKey="clicks" name="Clicks" fill={BLUE}
                      barSize={16} radius={[0, 4, 4, 0]} isAnimationActive={false}
                    >
                      <LabelList
                        dataKey="clicks" position="right" offset={10}
                        fill={AXIS_TEXT} fontSize={12}
                        formatter={(v) => nf.format(v)}
                      />
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
                <TableView
                  caption="Clicks per link"
                  columns={['Link', 'Clicks']}
                  rows={perLink.map((l) => [l.title, l.clicks])}
                />
              </>
            )}
          </section>
        </div>
      </div>
    </div>
  );
}

const styles = `
  .an-screen {
    position: fixed;
    inset: 0;
    overflow-y: auto;
    background: #1e2127;
    font-family: system-ui, 'Segoe UI', Roboto, sans-serif;
    color: #e8eaee;
  }
  .an-container {
    width: 100%;
    max-width: 1280px;
    margin: 0 auto;
    padding: 24px 20px 64px;
    box-sizing: border-box;
    text-align: left;
  }
  .an-back {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 8px 14px 8px 10px;
    font-size: 14px;
    color: #d3d7dd;
    text-decoration: none;
    background: #2b2f37;
    border: 1px solid #3a3e46;
    border-radius: 10px;
    transition: background 0.2s, color 0.2s;
  }
  .an-back:hover {
    background: #333944;
    color: #f2f4f7;
  }
  .an-title {
    margin: 24px 0;
    font-size: 40px;
    font-weight: 700;
    letter-spacing: -0.8px;
    color: #f2f4f7;
  }
  .an-hero {
    padding: 22px 26px;
    margin-bottom: 20px;
    background: #23262d;
    border: 1px solid #2f333c;
    border-radius: 14px;
  }
  .an-hero-value {
    margin: 0;
    font-size: 34px;
    font-weight: 700;
    letter-spacing: -0.6px;
    color: #f2f4f7;
  }
  .an-hero-sub {
    margin: 4px 0 0;
    font-size: 15px;
    color: #9aa0ab;
  }
  .an-grid {
    display: grid;
    grid-template-columns: 1.35fr 1fr;
    gap: 20px;
    align-items: start;
  }
  .an-card {
    padding: 20px 22px 16px;
    background: #23262d;
    border: 1px solid #2f333c;
    border-radius: 14px;
    min-width: 0;
  }
  .an-card-title {
    margin: 0 0 16px;
    font-size: 20px;
    font-weight: 600;
    color: #f2f4f7;
  }
  .an-message {
    margin: 8px 0;
    font-size: 15px;
    color: #9aa0ab;
  }
  .an-message-error {
    color: #f87171;
  }
  .an-tooltip {
    padding: 8px 11px;
    font-size: 13px;
    color: #e8eaee;
    background: #2b2f37;
    border: 1px solid #3a3e46;
    border-radius: 8px;
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.35);
  }
  .an-tooltip-label {
    margin: 0 0 5px;
    font-weight: 600;
    color: #f2f4f7;
  }
  .an-tooltip-row {
    display: flex;
    align-items: center;
    gap: 6px;
    margin: 2px 0;
    color: #c8ccd3;
  }
  .an-tooltip-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
  }
  .an-table-toggle {
    margin-top: 14px;
    padding-top: 12px;
    border-top: 1px solid #2f333c;
  }
  .an-table-toggle > summary {
    font-size: 13px;
    color: #9aa0ab;
    cursor: pointer;
    list-style: none;
  }
  .an-table-toggle > summary::-webkit-details-marker {
    display: none;
  }
  .an-table-toggle > summary::before {
    content: '▸ ';
  }
  .an-table-toggle[open] > summary::before {
    content: '▾ ';
  }
  .an-table-toggle > summary:hover {
    color: #e8eaee;
  }
  .an-table-scroll {
    max-height: 240px;
    margin-top: 10px;
    overflow: auto;
  }
  .an-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 13px;
    font-variant-numeric: tabular-nums;
  }
  .an-table th,
  .an-table td {
    padding: 7px 10px;
    text-align: left;
    font-weight: 400;
    color: #c8ccd3;
    border-bottom: 1px solid #2f333c;
  }
  .an-table thead th {
    position: sticky;
    top: 0;
    font-weight: 600;
    color: #9aa0ab;
    background: #23262d;
  }
  .an-table td {
    text-align: right;
    color: #e8eaee;
  }
  .an-visually-hidden {
    position: absolute;
    width: 1px;
    height: 1px;
    padding: 0;
    margin: -1px;
    overflow: hidden;
    clip-path: inset(50%);
    white-space: nowrap;
  }
  @media (max-width: 900px) {
    .an-grid {
      grid-template-columns: 1fr;
    }
  }
  @media (max-width: 600px) {
    .an-container {
      padding: 18px 14px 48px;
    }
    .an-title {
      font-size: 27px;
      margin: 18px 0;
    }
    .an-hero {
      padding: 18px;
    }
    .an-hero-value {
      font-size: 25px;
    }
    .an-card {
      padding: 16px 14px 12px;
    }
    .an-card-title {
      font-size: 17px;
    }
  }
`;
