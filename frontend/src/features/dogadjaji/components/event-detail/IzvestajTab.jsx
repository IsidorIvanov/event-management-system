import { useState, useEffect, useRef } from 'react';
import * as analitikaApi from '@/features/dogadjaji/services/analitikaService';
import { useToast } from '@/shared/components/ToastNotification';

function ChartTooltip({ tip }) {
  if (!tip) return null;
  return (
    <div className="izvestaj-tooltip" style={{ left: tip.x, top: tip.y }}>
      <div className="izvestaj-tooltip-label">{tip.label}</div>
      <div className="izvestaj-tooltip-value">{tip.value}</div>
    </div>
  );
}

function LineChart({ points, unit = '' }) {
  const wrapRef = useRef(null);
  const [tip, setTip] = useState(null);

  if (!points || points.length === 0) {
    return <div className="izvestaj-chart-empty">Nema podataka o registracijama.</div>;
  }

  const W = 560, H = 240, padX = 36, padTop = 16, padBottom = 28;
  const plotW = W - padX - 12;
  const plotH = H - padTop - padBottom;
  const max = Math.max(1, ...points.map((p) => p.vrednost));
  const n = points.length;
  const xAt = (i) => padX + (n === 1 ? plotW / 2 : (plotW * i) / (n - 1));
  const yAt = (v) => padTop + plotH - (plotH * v) / max;

  const path = points.map((p, i) => `${i === 0 ? 'M' : 'L'} ${xAt(i).toFixed(1)} ${yAt(p.vrednost).toFixed(1)}`).join(' ');
  const area = `${path} L ${xAt(n - 1).toFixed(1)} ${(padTop + plotH).toFixed(1)} L ${xAt(0).toFixed(1)} ${(padTop + plotH).toFixed(1)} Z`;
  const step = Math.max(1, Math.ceil(n / 8));

  const showTip = (e, p) => {
    const rect = wrapRef.current?.getBoundingClientRect();
    if (!rect) return;
    setTip({
      x: e.clientX - rect.left,
      y: e.clientY - rect.top,
      label: p.oznaka,
      value: `Prisustvo: ${p.vrednost}${unit}`,
    });
  };

  return (
    <div className="izvestaj-chart-wrap" ref={wrapRef}>
      <svg className="izvestaj-svg" viewBox={`0 0 ${W} ${H}`} preserveAspectRatio="xMidYMid meet">
        {[0, 0.25, 0.5, 0.75, 1].map((f) => {
          const y = padTop + plotH * f;
          return (
            <g key={f}>
              <line x1={padX} y1={y} x2={W - 12} y2={y} className="izvestaj-grid" />
              <text x={padX - 6} y={y + 3} textAnchor="end" className="izvestaj-axis">{Math.round(max * (1 - f))}</text>
            </g>
          );
        })}
        <path d={area} className="izvestaj-area" />
        <path d={path} className="izvestaj-line" />
        {points.map((p, i) => (
          <g key={i}>
            <circle
              cx={xAt(i)}
              cy={yAt(p.vrednost)}
              r="2.5"
              className={`izvestaj-dot${tip && tip.label === p.oznaka ? ' is-active' : ''}`}
            />
            {(i % step === 0 || i === n - 1) && (
              <text x={xAt(i)} y={H - 8} textAnchor="middle" className="izvestaj-axis">{p.oznaka}</text>
            )}
            <circle
              cx={xAt(i)}
              cy={yAt(p.vrednost)}
              r="10"
              className="izvestaj-hit"
              onMouseEnter={(e) => showTip(e, p)}
              onMouseMove={(e) => showTip(e, p)}
              onMouseLeave={() => setTip(null)}
            />
          </g>
        ))}
      </svg>
      <ChartTooltip tip={tip} />
    </div>
  );
}

function BarChart({ points, unit = '' }) {
  const wrapRef = useRef(null);
  const [tip, setTip] = useState(null);

  if (!points || points.length === 0) {
    return <div className="izvestaj-chart-empty">Nema sesija za ovaj događaj.</div>;
  }

  const W = 560, H = 240, padX = 36, padTop = 18, padBottom = 34;
  const plotW = W - padX - 12;
  const plotH = H - padTop - padBottom;
  const max = Math.max(1, ...points.map((p) => p.vrednost));
  const n = points.length;
  const slot = plotW / n;
  const barW = Math.min(46, slot * 0.6);
  const baseY = padTop + plotH;
  const trim = (s) => (s && s.length > 12 ? `${s.slice(0, 11)}…` : s || '');

  const showTip = (e, p) => {
    const rect = wrapRef.current?.getBoundingClientRect();
    if (!rect) return;
    setTip({
      x: e.clientX - rect.left,
      y: e.clientY - rect.top,
      label: p.oznaka,
      value: `Engagement: ${p.vrednost}${unit}`,
    });
  };

  return (
    <div className="izvestaj-chart-wrap" ref={wrapRef}>
      <svg className="izvestaj-svg" viewBox={`0 0 ${W} ${H}`} preserveAspectRatio="xMidYMid meet">
        {[0, 0.25, 0.5, 0.75, 1].map((f) => {
          const y = padTop + plotH * f;
          return (
            <g key={f}>
              <line x1={padX} y1={y} x2={W - 12} y2={y} className="izvestaj-grid" />
              <text x={padX - 6} y={y + 3} textAnchor="end" className="izvestaj-axis">{Math.round(max * (1 - f))}</text>
            </g>
          );
        })}
        {points.map((p, i) => {
          const cx = padX + slot * i + slot / 2;
          const h = (plotH * p.vrednost) / max;
          const active = tip && tip.label === p.oznaka;
          return (
            <g
              key={i}
              onMouseEnter={(e) => showTip(e, p)}
              onMouseMove={(e) => showTip(e, p)}
              onMouseLeave={() => setTip(null)}
            >
              <rect x={padX + slot * i} y={padTop} width={slot} height={plotH} className="izvestaj-hit" />
              <rect x={cx - barW / 2} y={baseY - h} width={barW} height={h} className={`izvestaj-bar${active ? ' is-active' : ''}`} rx="2" />
              <text x={cx} y={baseY - h - 4} textAnchor="middle" className="izvestaj-axis">{p.vrednost}{unit}</text>
              <text x={cx} y={H - 8} textAnchor="middle" className="izvestaj-axis">{trim(p.oznaka)}</text>
            </g>
          );
        })}
      </svg>
      <ChartTooltip tip={tip} />
    </div>
  );
}

export default function IzvestajTab({ event }) {
  const toast = useToast();
  const [analitika, setAnalitika] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [downloading, setDownloading] = useState(false);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(false);
    analitikaApi.getAnalitika(event.dogadjajId)
      .then((res) => { if (active) setAnalitika(res.data); })
      .catch(() => { if (active) setError(true); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [event.dogadjajId]);

  const handleDownload = async () => {
    setDownloading(true);
    try {
      const safeName = (event.naziv || 'dogadjaj').replace(/[^\p{L}\p{N}_-]+/gu, '-').slice(0, 60);
      await analitikaApi.downloadPdf(event.dogadjajId, `izvestaj-${safeName}.pdf`);
    } catch {
      toast('Greška pri generisanju PDF izveštaja.', 'error');
    } finally {
      setDownloading(false);
    }
  };

  if (loading) return <p className="empty-hint">Učitavanje izveštaja...</p>;
  if (error) return <p className="empty-hint" style={{ color: 'var(--danger)' }}>Greška pri učitavanju izveštaja.</p>;
  if (!analitika) return null;

  return (
    <div className="izvestaj-tab">
      <div className="izvestaj-header">
        <div>
          <h2 className="izvestaj-title">Reports &amp; Analytics</h2>
        </div>
        <button className="btn btn-primary" style={{ width: 'auto' }} onClick={handleDownload} disabled={downloading}>
          {downloading ? 'Generisanje...' : 'Generiši PDF izveštaj'}
        </button>
      </div>

      {/* Summary */}
      <div className="sesije-stats" style={{ marginBottom: '1.25rem' }}>
        <div className="sesija-stat-card">
          <div className="sesija-stat-label">Stopa prisustva</div>
          <div className="sesija-stat-value">{analitika.stopaPrisustva}%</div>
        </div>
        <div className="sesija-stat-card">
          <div className="sesija-stat-label">Potvrđene registracije</div>
          <div className="sesija-stat-value">{analitika.ukupnoRegistracija} / {analitika.maksKapacitet}</div>
        </div>
        <div className="sesija-stat-card">
          <div className="sesija-stat-label">Prosečan engagement</div>
          <div className="sesija-stat-value">{analitika.prosecniEngagement}%</div>
        </div>
        <div className="sesija-stat-card">
          <div className="sesija-stat-label">Broj sesija</div>
          <div className="sesija-stat-value">{analitika.engagementPoSesiji?.length ?? 0}</div>
        </div>
      </div>

      <div className="izvestaj-charts">
        <div className="events-table-card izvestaj-chart-card">
          <h3 className="izvestaj-chart-title">Attendance Rate</h3>
          <LineChart points={analitika.stopaPrisustvaSerija} unit="%" />
        </div>
        <div className="events-table-card izvestaj-chart-card">
          <h3 className="izvestaj-chart-title">Engagement Score / Session</h3>
          <BarChart points={analitika.engagementPoSesiji} unit="%" />
        </div>
      </div>
    </div>
  );
}
