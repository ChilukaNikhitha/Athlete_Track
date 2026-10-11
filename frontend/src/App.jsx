import React, { useEffect, useState } from 'react';
import {
  Activity, Apple, ArrowRight, Bell, Check, ChevronDown, CircleHelp,
  Dumbbell, Eye, EyeOff, HeartPulse, LayoutDashboard, LogOut, Menu, MessageSquare,
  Plus, Settings, ShieldCheck, Trophy, UserRound, Users, X,
} from 'lucide-react';
import { apiRequest } from './api.js';

const navigation = {
  PLAYER: [
    ['Overview', LayoutDashboard], ['My profile', UserRound], ['Tournament schedule', Trophy], ['Performance', Activity],
    ['Training plan', Dumbbell], ['Nutrition', Apple], ['Wellness', HeartPulse], ['Feedback', MessageSquare],
  ],
  COACH: [
    ['Overview', LayoutDashboard], ['My players', Users], ['Tournaments', Trophy],
    ['Training plans', Dumbbell], ['Nutrition plans', Apple], ['Feedback', MessageSquare],
  ],
};

const authSessionKey = 'athletetrack.auth';
const numericFieldConstraints = {
  age: { min: 10, max: 100, step: 1 },
  heightCm: { min: 80, max: 250, step: 'any' },
  weightKg: { min: 20, max: 350, step: 'any' },
  playerId: { min: 1, step: 1 },
  value: { min: 0, step: 'any' },
  sleepHours: { min: 0, max: 24, step: 'any' },
  recoveryScore: { min: 0, max: 100, step: 1 },
  energyLevel: { min: 1, max: 5, step: 1 },
};
const textFieldMaxLengths = {
  sport: 80,
  goal: 500,
  metric: 80,
  unit: 30,
  notes: 500,
  title: 120,
  details: 2000,
  message: 2000,
  tournamentName: 120,
  opponent: 120,
  venue: 120,
  stage: 80,
};

function readAuthSession() {
  const stored = window.sessionStorage.getItem(authSessionKey);
  if (!stored) return null;
  try {
    const value = JSON.parse(stored);
    if (typeof value?.token === 'string' && typeof value.account?.id === 'number'
      && typeof value.account.email === 'string' && typeof value.account.displayName === 'string'
      && ['PLAYER', 'COACH'].includes(value.account.role)) return value;
  } catch (problem) {
    if (!(problem instanceof SyntaxError)) throw problem;
  }
  window.sessionStorage.removeItem(authSessionKey);
  return null;
}

function NextTournament({ token, onNavigate }) {
  const [fixtures, setFixtures] = useState(null);
  const [error, setError] = useState('');
  useEffect(() => {
    if (!token) {
      setFixtures(null);
      setError('');
      return undefined;
    }
    let cancelled = false;
    apiRequest('/api/tournaments', { token }).then((result) => {
      if (!cancelled) setFixtures(result);
    }).catch((problem) => {
      if (!cancelled) setError(problem.message);
    });
    return () => { cancelled = true; };
  }, [token]);
  const nextFixture = fixtures?.find((fixture) => new Date(fixture.scheduledAt) >= new Date());
  return <div className="sidebar-match">
    <small>{token ? 'NEXT FIXTURE' : 'TOURNAMENTS'}</small>
    {nextFixture ? <>
      <div className="team-badges"><b>AT</b><strong>VS</strong><b>{nextFixture.opponent.slice(0, 2).toUpperCase()}</b></div>
      <strong>{nextFixture.tournamentName}</strong>
      <span>{new Date(nextFixture.scheduledAt).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' })}</span>
      <span>{nextFixture.venue}</span>
    </> : <strong>{error ? 'Schedule unavailable' : fixtures ? 'No upcoming fixtures' : token ? 'Loading schedule…' : 'View your schedule'}</strong>}
    <button type="button" onClick={() => onNavigate(token ? 'Tournament schedule' : 'Sign in')}>
      {token ? 'Open schedule' : 'Sign in'}
    </button>
  </div>;
}

function Overview({ account, role, token, onNavigate }) {
  const [summary, setSummary] = useState(null);
  const [summaryError, setSummaryError] = useState('');
  useEffect(() => {
    setSummary(null);
    setSummaryError('');
    if (!token) return undefined;
    let cancelled = false;
    const requests = role === 'COACH'
      ? [apiRequest('/api/coach/players', { token })]
      : [apiRequest('/api/performance', { token }), apiRequest('/api/wellness', { token })];
    Promise.all(requests).then((results) => {
      if (cancelled) return;
      setSummary(role === 'COACH' ? { players: results[0] } : { performance: results[0], wellness: results[1] });
    }).catch((problem) => {
      if (!cancelled) setSummaryError(problem.message);
    });
    return () => { cancelled = true; };
  }, [role, token]);
  const firstName = account?.displayName?.split(' ')[0] || (role === 'COACH' ? 'Coach' : 'Arjun');
  const isCoach = role === 'COACH';
  const shortcuts = isCoach
    ? [
      { label: 'My players', detail: 'View and assign players', Icon: Users },
      { label: 'Training plans', detail: 'Create plans for your team', Icon: Dumbbell },
      { label: 'Feedback', detail: 'Send feedback to players', Icon: MessageSquare },
    ]
    : [
      { label: 'My profile', detail: 'Update your personal details', Icon: UserRound },
      { label: 'Performance', detail: 'Review your performance records', Icon: Activity },
      { label: 'Wellness', detail: 'Track your recovery and wellbeing', Icon: HeartPulse },
    ];
  return <section className="simple-dashboard">
    <header className="simple-dashboard-heading">
      <span className="eyebrow">{isCoach ? 'COACH WORKSPACE' : 'PLAYER DASHBOARD'}</span>
      <h1>Welcome back, {firstName}</h1>
      <p>{isCoach ? 'Manage your players and coaching tasks from one place.' : 'A simple overview of your training and wellbeing.'}</p>
    </header>
    <section className="dashboard-summary">
      <h2>{isCoach ? 'Team snapshot' : 'Your recent activity'}</h2>
      {!token ? <p className="summary-message">Sign in to see your saved records.</p>
        : summaryError ? <p className="summary-error">{summaryError}</p>
          : !summary ? <p className="summary-message">Loading your latest records…</p>
            : isCoach ? <article className="summary-stat"><span className="simple-shortcut-icon"><Users size={19} /></span><div><small>Players assigned to you</small><strong>{summary.players.length}</strong></div><button className="text-button" onClick={() => onNavigate('My players')}>View players <ArrowRight size={14} /></button></article>
              : <div className="summary-stats">
                <article className="summary-stat"><span className="simple-shortcut-icon"><Activity size={19} /></span><div><small>Recent performance</small><strong>{summary.performance.length} {summary.performance.length === 1 ? 'record' : 'records'}</strong><em>{summary.performance[0] ? `${summary.performance[0].metric}: ${summary.performance[0].value} ${summary.performance[0].unit}` : 'Log your first result'}</em></div></article>
                <article className="summary-stat"><span className="simple-shortcut-icon"><HeartPulse size={19} /></span><div><small>Latest wellness check-in</small><strong>{summary.wellness[0] ? `Recovery ${summary.wellness[0].recoveryScore}/100` : 'No check-ins yet'}</strong><em>{summary.wellness[0] ? `${summary.wellness[0].sleepHours} hours sleep · Energy ${summary.wellness[0].energyLevel}/5` : 'Submit a check-in to get started'}</em></div></article>
              </div>}
    </section>
    <h2 className="simple-section-title">{isCoach ? 'Your coaching tools' : 'Get started'}</h2>
    <div className="simple-shortcuts">
      {shortcuts.map(({ label, detail, Icon }) => <button className="simple-shortcut" key={label} onClick={() => onNavigate(label)}>
        <span className="simple-shortcut-icon"><Icon size={19} /></span>
        <span className="simple-shortcut-copy"><strong>{label}</strong><small>{detail}</small></span>
        <ArrowRight size={16} />
      </button>)}
    </div>
  </section>;
}

function AuthDialog({ close, signedIn, role }) {
  const [register, setRegister] = useState(false);
  const [selectedRole, setSelectedRole] = useState(role);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [registrationCode, setRegistrationCode] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const selectRole = (nextRole) => {
    setSelectedRole(nextRole);
    setRegister(false);
    setError('');
  };
  const submit = async (event) => {
    event.preventDefault();
    setBusy(true);
    setError('');
    try {
      const endpoint = register ? selectedRole === 'COACH' ? 'register-coach' : 'register' : 'login';
      const body = register
        ? { displayName: name, email, password, ...(selectedRole === 'COACH' ? { registrationCode } : {}) }
        : { email, password, role: selectedRole };
      const result = await apiRequest(`/api/auth/${endpoint}`, {
        method: 'POST',
        body,
      });
      signedIn(result);
    } catch (problem) {
      setError(problem.message);
    } finally {
      setBusy(false);
    }
  };
  return <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && close()}>
    <section className="modal" role="dialog" aria-modal="true" aria-labelledby="auth-heading">
      <button className="icon close-modal" onClick={close} aria-label="Close"><X size={18} /></button>
      <div className="logo"><Activity size={21} /> Athlete<span>Track</span></div>
      <div className="auth-role-select" aria-label="Account type">
        {['PLAYER', 'COACH'].map((accountRole) => <button key={accountRole} type="button"
          className={selectedRole === accountRole ? 'selected' : ''}
          aria-pressed={selectedRole === accountRole}
          onClick={() => selectRole(accountRole)}>
          {accountRole === 'PLAYER' ? 'Player' : 'Coach'}
        </button>)}
      </div>
      <span className="eyebrow">{register ? 'GET STARTED' : 'WELCOME BACK'}</span>
      <h2 id="auth-heading">{register ? `Create your ${selectedRole === 'COACH' ? 'coach' : 'player'} account` : `Sign in as ${selectedRole === 'COACH' ? 'a coach' : 'a player'}`}</h2>
      <p>{register && selectedRole === 'COACH'
        ? 'Enter the coach invitation code provided by your administrator.'
        : selectedRole === 'COACH' ? 'Use your coach account or create one with an administrator-provided invitation code.' : 'Keep your performance and wellness journey on track.'}</p>
      <form onSubmit={submit}>
        {register && <label>Full name<input required maxLength={100} autoComplete="name" value={name} onChange={(event) => setName(event.target.value)} /></label>}
        <label>Email address<input required type="email" maxLength={254} autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} placeholder="you@example.com" /></label>
        <label>Password<div className="password-input-wrap"><input required type={showPassword ? 'text' : 'password'} minLength={register ? 8 : 1} maxLength={72} autoComplete={register ? 'new-password' : 'current-password'} value={password} onChange={(event) => setPassword(event.target.value)} /><button className="password-visibility-toggle" type="button" aria-label={showPassword ? 'Hide password' : 'Show password'} aria-pressed={showPassword} onClick={() => setShowPassword(!showPassword)}>{showPassword ? <EyeOff size={17} /> : <Eye size={17} />}</button></div></label>
        {register && selectedRole === 'COACH' && <label>Coach invitation code<input required type="password" maxLength={256} autoComplete="off" value={registrationCode} onChange={(event) => setRegistrationCode(event.target.value)} /></label>}
        {error && <div className="error">{error}</div>}
        <button className="primary full" disabled={busy}>{busy ? 'Please wait…' : register ? 'Create account' : 'Sign in'}</button>
      </form>
      <div className="switch-auth">{register ? 'Already have an account?' : 'New to AthleteTrack?'} <button onClick={() => { setRegister(!register); setError(''); }}>{register ? 'Sign in' : selectedRole === 'COACH' ? 'Create coach account' : 'Create account'}</button></div>
    </section>
  </div>;
}

function Profile({ token, account }) {
  const [data, setData] = useState(null);
  const [edit, setEdit] = useState(false);
  const [error, setError] = useState('');
  const [form, setForm] = useState({ age: '', heightCm: '', weightKg: '', sport: '', goal: '' });
  const describeProfileError = async (problem) => {
    if (problem.status !== 403) return problem.message;
    try {
      const session = await apiRequest('/api/auth/me', { token });
      if (session.role !== 'PLAYER') {
        return `The API identifies this account as ${session.role}, not Player. Sign out and sign in with a Player account, or ask your administrator to correct this account's role.`;
      }
      return 'The API confirms this account has the Player role, but the profile endpoint rejected it. Restart the backend from this project and try again; if it continues, check that the frontend and backend are connected to the same API.';
    } catch (diagnosticProblem) {
      if (diagnosticProblem.status === 401) {
        return 'The API did not authenticate this browser session. You have been signed out; sign in again as Player and retry.';
      }
      return `The API denied player profile access, and its session role could not be checked (${diagnosticProblem.message}). Restart the backend from this project, sign in again, and retry.`;
    }
  };
  useEffect(() => {
    let cancelled = false;
    apiRequest('/api/profile', { token }).then((profile) => {
      if (cancelled) return;
      setData(profile);
      setForm({ age: profile.age ?? '', heightCm: profile.heightCm ?? '', weightKg: profile.weightKg ?? '', sport: profile.sport ?? '', goal: profile.goal ?? '' });
    }).catch(async (problem) => {
      const message = await describeProfileError(problem);
      if (!cancelled) setError(message);
    });
    return () => { cancelled = true; };
  }, [token]);
  const save = async (event) => {
    event.preventDefault();
    setError('');
    try {
      const updated = await apiRequest('/api/profile', { method: 'PUT', token, body: {
        age: form.age === '' ? null : Number(form.age),
        heightCm: form.heightCm === '' ? null : Number(form.heightCm),
        weightKg: form.weightKg === '' ? null : Number(form.weightKg),
        sport: form.sport, goal: form.goal,
      } });
      setData(updated);
      setEdit(false);
    } catch (problem) { setError(await describeProfileError(problem)); }
  };
  const fields = [['age', 'Age'], ['heightCm', 'Height (cm)'], ['weightKg', 'Weight (kg)'], ['sport', 'Sport'], ['goal', 'Wellness goals']];
  return <section className="page">
    <div className="page-title"><div><span className="eyebrow">YOUR DETAILS</span><h1>Player profile</h1><p>Keep your personal and fitness information up to date.</p></div>{!edit && <button className="secondary" onClick={() => setEdit(true)}>Edit profile</button>}</div>
    {error && <div className="error">{error}</div>}
    {edit && <form className="panel form-grid" onSubmit={save}>{fields.map(([key, label]) => <label key={key}>{label}<input value={form[key]} onChange={(event) => setForm({ ...form, [key]: event.target.value })} type={['age', 'heightCm', 'weightKg'].includes(key) ? 'number' : 'text'} {...numericFieldConstraints[key]} {...(textFieldMaxLengths[key] ? { maxLength: textFieldMaxLengths[key] } : {})} /></label>)}<div className="form-actions"><button type="button" className="secondary" onClick={() => setEdit(false)}>Cancel</button><button className="primary">Save profile</button></div></form>}
    <div className="cards">
      <article className="panel info-card"><UserRound /><div><strong>{data?.displayName || account.displayName}</strong><p>{data?.email || account.email} · Player ID #{data?.userId || account.id} · {data?.sport || 'Sport not set'} · {data?.age ? `Age ${data.age}` : 'Age not set'}</p></div></article>
      <article className="panel info-card"><Activity /><div><strong>Fitness measurements</strong><p>{data?.heightCm ? `${data.heightCm} cm` : 'Height not set'} · {data?.weightKg ? `${data.weightKg} kg` : 'Weight not set'} · BMI {data?.bmi ?? '—'}</p></div></article>
      <article className="panel info-card"><HeartPulse /><div><strong>Wellness goals</strong><p>{data?.goal || 'Add your wellness goals'}</p></div></article>
    </div>
  </section>;
}

function SettingsPage({ account, role, onNavigate, onSignIn, onSignOut }) {
  return <section className="page utility-page">
    <div className="page-title"><div><span className="eyebrow">PREFERENCES</span><h1>Settings</h1><p>View your account details and manage your workspace.</p></div></div>
    <article className="panel utility-card">
      <div className="utility-card-heading"><span className="utility-icon"><UserRound size={19} /></span><div><strong>Account</strong><small>Your AthleteTrack sign-in</small></div></div>
      <dl className="account-details">
        <div><dt>Name</dt><dd>{account?.displayName || 'Sample account'}</dd></div>
        <div><dt>Email</dt><dd>{account?.email || 'Not signed in'}</dd></div>
        <div><dt>Workspace</dt><dd>{role === 'COACH' ? 'Coach' : 'Player'}</dd></div>
      </dl>
      <div className="utility-actions">
        {account ? <>
          {role === 'PLAYER' && <button className="secondary" onClick={() => onNavigate('My profile')}>Edit player profile</button>}
          <button className="secondary sign-out-button" onClick={onSignOut}><LogOut size={15} />Sign out</button>
        </> : <button className="primary" onClick={onSignIn}>Sign in</button>}
      </div>
    </article>
    <article className="panel utility-card">
      <div className="utility-card-heading"><span className="utility-icon"><ShieldCheck size={19} /></span><div><strong>Account access</strong><small>Roles determine which AthleteTrack features are available.</small></div></div>
      <p className="utility-copy">{role === 'COACH'
        ? 'Coach access is managed by an administrator. Contact your administrator if you need help with your coach account.'
        : 'Player accounts can update a profile, log performance, and submit wellness check-ins.'}</p>
      <button className="text-button" onClick={() => onNavigate('Help center')}>Read the help center <ArrowRight size={14} /></button>
    </article>
  </section>;
}

function HelpCenter({ account, role, onNavigate, onSignIn }) {
  const topics = [
    { question: 'How do I update my profile?', answer: 'Open My profile from the menu to review or edit your player details.', action: role === 'PLAYER' ? 'My profile' : null },
    { question: 'How do I access coach features?', answer: 'Register with the private invitation code from your administrator when coach registration is enabled, or ask an administrator to enable your account. Then sign in using the Coach option.', action: null },
    { question: 'Where can I find my records and tournament schedule?', answer: role === 'COACH'
      ? 'Use My players, Tournaments, Training plans, or Feedback in the menu to manage your team.'
      : 'Use Tournament schedule, Performance, Wellness, Training plan, or Feedback in the menu to view your records.', action: role === 'COACH' ? 'Tournaments' : 'Tournament schedule' },
  ];
  return <section className="page utility-page">
    <div className="page-title"><div><span className="eyebrow">ATHLETETRACK SUPPORT</span><h1>Help center</h1><p>Quick answers to common account and dashboard questions.</p></div></div>
    <div className="help-topics">{topics.map(({ question, answer, action }) => <article className="panel help-topic" key={question}>
      <span className="utility-icon"><CircleHelp size={19} /></span>
      <div><h2>{question}</h2><p>{answer}</p>{action && <button className="text-button" onClick={() => onNavigate(action)}>Open {action} <ArrowRight size={14} /></button>}</div>
    </article>)}</div>
    <article className="panel utility-card help-signin">
      <div><strong>{account ? 'Manage your account' : 'Need to connect your account?'}</strong><p>{account ? 'Review your account details and sign-out options.' : 'Sign in to access the tools for your player or coach workspace.'}</p></div>
      {account ? <button className="secondary" onClick={() => onNavigate('Settings')}>Account settings</button> : <button className="primary" onClick={onSignIn}>Sign in</button>}
    </article>
  </section>;
}

function CoachPlayers({ token, notify }) {
  const [players, setPlayers] = useState(null);
  const [selected, setSelected] = useState(null);
  const [performance, setPerformance] = useState(null);
  const [playerEmail, setPlayerEmail] = useState('');
  const [showAssign, setShowAssign] = useState(false);
  const [error, setError] = useState('');
  useEffect(() => {
    let cancelled = false;
    apiRequest('/api/coach/players', { token }).then((result) => {
      if (!cancelled) setPlayers(result);
    }).catch((problem) => {
      if (!cancelled) setError(problem.message);
    });
    return () => { cancelled = true; };
  }, [token]);
  useEffect(() => {
    if (!selected) {
      setPerformance(null);
      return undefined;
    }
    let cancelled = false;
    setPerformance(null);
    setError('');
    apiRequest(`/api/coach/players/${selected.id}/performance`, { token }).then((result) => {
      if (!cancelled) setPerformance(result);
    }).catch((problem) => {
      if (!cancelled) setError(problem.message);
    });
    return () => { cancelled = true; };
  }, [selected, token]);
  const assign = async (event) => {
    event.preventDefault();
    setError('');
    try {
      const added = await apiRequest('/api/coach/players', {
        method: 'POST', token, body: { email: playerEmail.trim() },
      });
      setPlayers(await apiRequest('/api/coach/players', { token }));
      setShowAssign(false);
      setPlayerEmail('');
      notify(`${added.displayName} was assigned to your team.`);
    } catch (problem) {
      setError(problem.message);
    }
  };
  return <section className="page">
    <div className="page-title"><div><span className="eyebrow">COACH WORKSPACE</span><h1>{selected ? selected.displayName : 'Your players'}</h1><p>{selected ? 'Performance records shared by this assigned player.' : 'Manage your roster and review assigned player performance.'}</p></div>
      {selected ? <button className="secondary" onClick={() => setSelected(null)}>Back to players</button> : <button className="primary" onClick={() => setShowAssign(!showAssign)}><Plus size={16} />Assign player</button>}
    </div>
    {error && <div className="error">{error}</div>}
    {showAssign && !selected && <form className="panel form-grid" onSubmit={assign}>
      <label>Player’s registered email<input required type="email" maxLength={254} autoComplete="email" placeholder="player@example.com" value={playerEmail} onChange={(event) => setPlayerEmail(event.target.value)} /></label>
      <p className="form-hint">Enter the email the player used to create their Player account. Ask them for it if you don’t know it.</p>
      <div className="form-actions"><button type="button" className="secondary" onClick={() => setShowAssign(false)}>Cancel</button><button className="primary">Assign player</button></div>
    </form>}
    {selected ? performance === null && !error
      ? <p className="summary-message">Loading performance records…</p>
      : performance?.length ? <div className="cards">{performance.map((entry) => <article className="panel info-card" key={entry.id}><Activity /><div><strong>{entry.metric}</strong><p>{entry.value} {entry.unit} · {entry.recordedAt}{entry.notes ? ` · ${entry.notes}` : ''}</p></div></article>)}</div>
        : !error && <p className="summary-message">This player hasn’t logged any performance records yet.</p>
      : players === null && !error ? <p className="summary-message">Loading your players…</p>
        : players?.length ? <div className="coach-player-list">{players.map((player) => <button className="panel coach-player-card" key={player.id} onClick={() => setSelected(player)}>
          <span className="simple-shortcut-icon"><UserRound size={19} /></span><span><strong>{player.displayName}</strong><small>{player.email} · Account ID {player.id}</small></span><ArrowRight size={16} />
        </button>)}</div>
          : !error && <p className="summary-message">No players assigned yet. Assign a player using their account ID.</p>}
  </section>;
}

function DataView({ section, role, token, notify, onSignIn }) {
  const config = {
    'Tournament schedule': { endpoint: '/api/tournaments', title: 'Tournament schedule' },
    Tournaments: { endpoint: '/api/tournaments', title: 'Tournament fixtures', action: 'Add fixture' },
    Performance: { endpoint: '/api/performance', title: 'My performance', action: 'Log performance' },
    Wellness: { endpoint: '/api/wellness', title: 'Wellness history', action: 'Submit check-in' },
    'My players': { endpoint: '/api/coach/players', title: 'Your players', action: 'Assign player' },
    'Training plan': { endpoint: '/api/plans', title: 'Training plan' },
    'Training plans': { endpoint: '/api/plans', title: 'Training plans', action: 'Create plan' },
    Nutrition: { endpoint: '/api/plans', title: 'Nutrition plan' },
    'Nutrition plans': { endpoint: '/api/plans', title: 'Nutrition plans', action: 'Create plan' },
    Feedback: { endpoint: '/api/feedback', title: 'Feedback', action: 'Send feedback' },
  }[section];
  const [items, setItems] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(Boolean(token));
  const [players, setPlayers] = useState([]);
  const [playersLoading, setPlayersLoading] = useState(false);
  const [playersError, setPlayersError] = useState('');
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState(false);
  const [values, setValues] = useState({});
  const canCreate = (role === 'PLAYER' && ['Performance','Wellness'].includes(section)) || (role === 'COACH' && ['My players','Tournaments','Training plans','Nutrition plans','Feedback'].includes(section));
  const needsPlayerSelection = role === 'COACH' && ['Tournaments','Training plans','Nutrition plans','Feedback'].includes(section);
  useEffect(() => {
    if (!token) {
      setItems(null);
      setLoading(false);
      return undefined;
    }
    let cancelled = false;
    setItems(null);
    setError('');
    setLoading(true);
    apiRequest(config.endpoint, { token }).then((result) => { if (!cancelled) setItems(result); })
      .catch((problem) => { if (!cancelled) setError(problem.message); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [config.endpoint, token]);
  useEffect(() => {
    if (!token || !needsPlayerSelection) {
      setPlayers([]);
      setPlayersLoading(false);
      setPlayersError('');
      return undefined;
    }
    let cancelled = false;
    setPlayers([]);
    setPlayersLoading(true);
    setPlayersError('');
    apiRequest('/api/coach/players', { token }).then((result) => {
      if (!cancelled) setPlayers(result);
    }).catch((problem) => {
      if (!cancelled) setPlayersError(problem.message);
    }).finally(() => {
      if (!cancelled) setPlayersLoading(false);
    });
    return () => { cancelled = true; };
  }, [needsPlayerSelection, token]);
  const create = async (event) => {
    event.preventDefault();
    if (!token) {
      onSignIn();
      return;
    }
    const body = section === 'Tournaments'
      ? { playerId: Number(values.playerId), tournamentName: values.tournamentName, opponent: values.opponent, scheduledAt: values.scheduledAt, venue: values.venue, stage: values.stage, notes: values.notes }
      : section === 'Performance'
      ? { metric: values.metric, value: Number(values.value), unit: values.unit, notes: values.notes }
      : section === 'Wellness'
        ? { sleepHours: Number(values.sleepHours), recoveryScore: Number(values.recoveryScore), energyLevel: Number(values.energyLevel), notes: values.notes }
        : section === 'My players' ? { email: values.email.trim() }
          : section === 'Feedback' ? { playerId: Number(values.playerId), message: values.message }
            : { playerId: Number(values.playerId), type: section.startsWith('Nutrition') ? 'NUTRITION' : 'TRAINING', title: values.title, details: values.details, scheduledFor: values.scheduledFor || null };
    const endpoint = section === 'My players' ? '/api/coach/players' : config.endpoint;
    setSaving(true);
    setError('');
    try {
      await apiRequest(endpoint, { method: 'POST', token, body });
      setItems(await apiRequest(config.endpoint, { token }));
      setForm(false);
      setValues({});
      notify('Saved successfully.');
    } catch (problem) { setError(problem.message); }
    finally { setSaving(false); }
  };
  const fields = section === 'Tournaments' ? [['playerId','Assigned player'],['tournamentName','Tournament'],['opponent','Opponent'],['scheduledAt','Match date and time'],['venue','Venue'],['stage','Round / stage'],['notes','Notes']]
    : section === 'Performance' ? [['metric','Metric'],['value','Value'],['unit','Unit'],['notes','Notes']]
    : section === 'Wellness' ? [['sleepHours','Sleep hours'],['recoveryScore','Recovery score (0-100)'],['energyLevel','Energy (1-5)'],['notes','Notes']]
      : section === 'My players' ? [['email','Player’s registered email']]
        : section === 'Feedback' ? [['playerId','Player account ID'],['message','Feedback']]
          : [['playerId','Player account ID'],['title','Plan title'],['details','Plan details'],['scheduledFor','Scheduled date']];
  const description = section === 'Tournaments'
    ? 'Create tournament fixtures and share the schedule with assigned players.'
    : section === 'Tournament schedule'
    ? 'View tournament fixtures shared with you by your coaches.'
    : role === 'COACH' ? 'Manage plans and feedback for players assigned to you.'
      : 'Your saved performance, training, wellness, and feedback records.';
  return <section className="page"><div className="page-title"><div><span className="eyebrow">ATHLETETRACK</span><h1>{config.title}</h1><p>{description}</p></div>{config.action && canCreate && <button className="primary" onClick={() => token ? setForm(!form) : onSignIn()}><Plus size={16} />{config.action}</button>}</div>
    {error && <div className="error">{error}</div>}
    {form && <form className="panel form-grid" onSubmit={create}>{fields.map(([key,label]) => <label key={key}>{label}{key === 'playerId' && needsPlayerSelection
      ? <select required value={values[key] || ''} disabled={playersLoading || !players.length} onChange={(event) => setValues({ ...values, [key]: event.target.value })}><option value="">{playersLoading ? 'Loading assigned players…' : players.length ? 'Choose an assigned player' : 'No assigned players'}</option>{players.map((player) => <option key={player.id} value={player.id}>{player.displayName} (ID {player.id})</option>)}</select>
      : ['details','message'].includes(key) ? <textarea required value={values[key] || ''} onChange={(event) => setValues({ ...values, [key]: event.target.value })} />
        : <input required={!['notes','scheduledFor','stage'].includes(key)} value={values[key] || ''} type={key === 'email' ? 'email' : key === 'scheduledFor' ? 'date' : key === 'scheduledAt' ? 'datetime-local' : ['value','playerId','sleepHours','recoveryScore','energyLevel'].includes(key) ? 'number' : 'text'} {...numericFieldConstraints[key]} {...(textFieldMaxLengths[key] ? { maxLength: textFieldMaxLengths[key] } : {})} onChange={(event) => setValues({ ...values, [key]: event.target.value })} />}</label>)}<div className="form-actions"><button type="button" className="secondary" disabled={saving} onClick={() => setForm(false)}>Cancel</button><button className="primary" disabled={saving || (needsPlayerSelection && (playersLoading || !players.length))}>{saving ? 'Saving…' : 'Save'}</button></div></form>}
    {!token ? <div className="empty-state"><Activity size={22} /><p>Sign in to view your saved records.</p><button className="primary" onClick={onSignIn}>Sign in</button></div>
      : loading ? <p className="summary-message">Loading your records…</p>
        : !error && items?.length === 0 ? <div className="empty-state"><Activity size={22} /><p>No records yet.</p>{canCreate && <button className="secondary" onClick={() => setForm(true)}>{config.action}</button>}</div>
          : items?.length ? <div className="cards">{items.map((item) => {
      const isTournament = ['Tournaments','Tournament schedule'].includes(section);
      const title = isTournament ? `${item.tournamentName} · vs ${item.opponent}` : section === 'Performance' ? item.metric : section === 'Wellness' ? `Wellness check-in · ${item.recordedAt}` : section === 'My players' ? item.displayName : section === 'Feedback' ? 'Coach feedback' : item.title || config.title;
      const detail = isTournament ? [item.scheduledAt?.replace('T', ' · '), item.venue, item.stage, role === 'COACH' ? `Player ID ${item.playerId}` : null, item.notes].filter(Boolean).join(' · ') : section === 'Performance' ? `${item.value} ${item.unit} · ${item.recordedAt}` : section === 'Wellness' ? `Sleep ${item.sleepHours} hrs · Recovery ${item.recoveryScore}/100 · Energy ${item.energyLevel}/5` : section === 'My players' ? `${item.email} · ID ${item.id}` : section === 'Feedback' ? item.message : item.details || 'Training and nutrition plan';
      return <article className="panel info-card" key={item.id}><Activity /><div><strong>{title}</strong><p>{detail}</p></div></article>;
    })}</div> : null}
    {playersError && <div className="error">{playersError}</div>}
  </section>;
}

export default function App() {
  const [auth, setAuth] = useState(readAuthSession);
  const [role, setRole] = useState(() => auth?.account.role || 'PLAYER');
  const [active, setActive] = useState('Overview');
  const [showAuth, setShowAuth] = useState(false);
  const [mobile, setMobile] = useState(false);
  const [toast, setToast] = useState('');
  const notify = (message) => { setToast(message); window.setTimeout(() => setToast(''), 3000); };
  const signOut = () => {
    window.sessionStorage.removeItem(authSessionKey);
    setAuth(null);
    setRole('PLAYER');
    setActive('Overview');
    setShowAuth(false);
    setMobile(false);
    notify('You have been signed out.');
  };
  useEffect(() => {
    const handleExpiredSession = () => {
      signOut();
      notify('Your session expired. Please sign in again.');
    };
    window.addEventListener('athletetrack:session-expired', handleExpiredSession);
    return () => window.removeEventListener('athletetrack:session-expired', handleExpiredSession);
  }, [auth]);
  const navigate = (section) => {
    if (!auth && section === 'My profile') {
      setShowAuth(true);
      return;
    }
    setActive(section);
    setMobile(false);
  };
  const login = (result) => {
    window.sessionStorage.setItem(authSessionKey, JSON.stringify(result));
    setAuth(result);
    setRole(result.account.role);
    setShowAuth(false);
    setActive('Overview');
  };
  const items = navigation[role];
  return <div className="app">
    {mobile && <button className="scrim" aria-label="Close menu" onClick={() => setMobile(false)} />}
    <aside className={`sidebar ${mobile ? 'open' : ''}`}>
      <div className="logo"><Activity size={23} /><div><strong>ATHLETE<span>TRACK</span></strong><small>PERFORMANCE &amp; WELLNESS</small></div></div>
      <span className="eyebrow side-label">WORKSPACE</span>
      <div className="role-box">{role === 'COACH' ? <ShieldCheck /> : <UserRound />}<div><strong>{role === 'COACH' ? 'Coach' : 'Player'} workspace</strong><small>{role === 'COACH' ? 'Team dashboard' : 'Personal dashboard'}</small></div></div>
      <span className="eyebrow side-label">MENU</span>
      <nav>{items.map(([label, Icon]) => <button key={label} className={`nav-item ${active === label ? 'active' : ''}`} onClick={() => navigate(label)}><Icon size={18} />{label}</button>)}</nav>
      <div className="sidebar-bottom">
        <NextTournament token={auth?.token} onNavigate={(section) => section === 'Sign in' ? setShowAuth(true) : navigate(section)} />
        <button className={`nav-item ${active === 'Settings' ? 'active' : ''}`} onClick={() => { setActive('Settings'); setMobile(false); }}><Settings size={18} />Settings</button><button className={`nav-item ${active === 'Help center' ? 'active' : ''}`} onClick={() => { setActive('Help center'); setMobile(false); }}><CircleHelp size={18} />Help center</button>
        {auth && <button className="nav-item sidebar-sign-out" onClick={signOut}><LogOut size={18} />Sign out</button>}
        <div className="sidebar-user"><span className="avatar">{auth?.account.displayName?.slice(0, 1) || (role === 'COACH' ? 'C' : 'P')}</span><div><strong>{auth?.account.displayName || (role === 'COACH' ? 'Coach demo' : 'Player demo')}</strong><small>{auth?.account.email || 'Sample dashboard'}</small></div></div>
      </div>
    </aside>
    <main className="main">
      <header className="topbar">
        <button className="icon mobile-toggle" onClick={() => setMobile(true)} aria-label="Open navigation"><Menu /></button>
        <div className="topbar-title"><strong>{active === 'Overview' ? 'Dashboard' : active}</strong><small>Welcome back, {auth?.account.displayName?.split(' ')[0] || (role === 'COACH' ? 'Coach' : 'Arjun')}! Keep pushing your limits.</small></div>
        <div className="topbar-actions"><button className="icon notification-button" aria-label="Notifications" onClick={() => notify('You’re all caught up!')}><Bell size={18} /><i /></button>
          {auth ? <button className="account-button" onClick={signOut} aria-label="Sign out" title="Sign out"><span className="avatar">{auth.account.displayName.slice(0, 1)}</span><span><strong>{auth.account.displayName}</strong><small>{role === 'COACH' ? 'Coach' : 'Player'}</small></span><LogOut size={14} /></button> : <><button className="secondary preview-role" onClick={() => setRole(role === 'PLAYER' ? 'COACH' : 'PLAYER')}><ChevronDown size={14} /> Preview {role === 'PLAYER' ? 'coach' : 'player'}</button><button className="primary" onClick={() => setShowAuth(true)}>Sign in</button></>}
        </div>
      </header>
      {active === 'Overview' ? <Overview account={auth?.account} role={role} token={auth?.token} onNavigate={navigate} /> : active === 'My profile' ? <Profile token={auth?.token} account={auth?.account || {displayName:'Sample Player',email:''}} /> : active === 'Settings' ? <SettingsPage account={auth?.account} role={role} onNavigate={navigate} onSignIn={() => setShowAuth(true)} onSignOut={signOut} /> : active === 'Help center' ? <HelpCenter account={auth?.account} role={role} onNavigate={navigate} onSignIn={() => setShowAuth(true)} /> : active === 'My players' && role === 'COACH' ? <CoachPlayers key={auth?.account.id} token={auth?.token} notify={notify} /> : <DataView key={`${active}-${auth?.account.id || 'sample'}`} section={active} role={role} token={auth?.token} notify={notify} onSignIn={() => setShowAuth(true)} />}
      <footer className="footer"><span>© {new Date().getFullYear()} AthleteTrack</span><span><i/> {auth ? 'Account connected' : 'Sample preview'}</span><button onClick={() => auth ? signOut() : setShowAuth(true)}>{auth ? 'Sign out' : 'Sign in'}</button></footer>
    </main>
    {showAuth && <AuthDialog close={() => setShowAuth(false)} signedIn={login} role={role} />}
    {toast && <div className="toast"><Check size={16}/>{toast}</div>}
  </div>;
}
