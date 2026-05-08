import streamlit as st
import pandas as pd
import plotly.graph_objects as go
from datetime import datetime, timedelta
import json, os

st.set_page_config(page_title="WeightTracker", page_icon="⚖️", layout="wide", initial_sidebar_state="collapsed")

st.markdown("""
<style>
@import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@300;400;500;600;700;800&display=swap');
*, body, html { font-family: 'Plus Jakarta Sans', sans-serif !important; }
#MainMenu, footer, header { visibility: hidden; }
.stApp { background: #ffffff; min-height: 100vh; }

.block-container {
  padding: 0 !important;
  max-width: 100% !important;
  margin: 0 !important;
}

.page-body { padding: 1.25rem 2rem 3rem 2rem; }
.inner-page { padding: 1.25rem 2rem 3rem 2rem; max-width: 560px; }

.nav-logo {
  font-size: 0.95rem; font-weight: 800; letter-spacing: -0.02em;
  background: linear-gradient(135deg, #7c3aed, #f97316);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
  white-space: nowrap;
}

/* Horizontal radio nav */
div[data-testid="stHorizontalBlock"] .stRadio > div {
  display: flex !important; flex-direction: row !important; gap: 0.25rem !important;
}
.stRadio > div > label {
  background: #f9fafb !important;
  border: 1.5px solid #f0edf8 !important;
  border-radius: 9px !important;
  padding: 0.35rem 0.85rem !important;
  cursor: pointer !important;
  font-size: 0.78rem !important;
  font-weight: 600 !important;
  color: #9ca3af !important;
  transition: all 0.15s !important;
}
.stRadio > div > label:has(input:checked) {
  background: linear-gradient(135deg,#7c3aed,#a855f7) !important;
  color: #fff !important;
  border-color: transparent !important;
  box-shadow: 0 2px 8px rgba(124,58,237,0.25) !important;
}
.stRadio [data-testid="stMarkdownContainer"] p { font-size: 0.78rem !important; margin: 0 !important; }
.stRadio input[type="radio"] { display: none !important; }

/* Dashboard layout */
.hero-card {
  background: linear-gradient(135deg, #faf5ff 0%, #fff7ed 100%);
  border-radius: 18px;
  border: 1.5px solid #ede9fe;
  padding: 1.1rem 1.3rem;
  min-height: 132px;
}
.hero-lbl { font-size: 0.62rem; font-weight: 700; color: #9ca3af; text-transform: uppercase; letter-spacing: 0.08em; margin-bottom: 0.25rem; }
.hero-weight { font-size: 2.8rem; font-weight: 800; color: #1f2937; line-height: 1; letter-spacing: -0.04em; }
.hero-unit { font-size: 0.95rem; font-weight: 600; color: #9ca3af; margin-left: 2px; }
.chip-good, .chip-bad, .chip-flat {
  display:inline-flex; align-items:center; gap:3px; font-size:0.68rem; font-weight:700;
  padding:2px 8px; border-radius:99px; margin-top:0.4rem;
}
.chip-good { background:#dcfce7; color:#16a34a; }
.chip-bad  { background:#fee2e2; color:#dc2626; }
.chip-flat { background:#f3f4f6; color:#6b7280; }

.stat-card {
  border-radius: 18px;
  padding: 1rem 1.1rem;
  min-height: 132px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.sc-purple { background: linear-gradient(135deg, #7c3aed, #a855f7); }
.sc-orange { background: linear-gradient(135deg, #f97316, #fb923c); }
.sc-dark   { background: linear-gradient(135deg, #1e1b4b, #312e81); }
.sc-soft   { background: #f9fafb; border: 1.5px solid #f0edf8; }
.sc-lbl    { font-size: 0.62rem; font-weight: 700; color: rgba(255,255,255,0.62); text-transform: uppercase; letter-spacing: 0.07em; }
.sc-lbl-d  { font-size: 0.62rem; font-weight: 700; color: #9ca3af; text-transform: uppercase; letter-spacing: 0.07em; }
.sc-val    { font-size: 1.3rem; font-weight: 800; color: #fff; line-height: 1.1; letter-spacing: -0.02em; margin-top: 0.2rem; }
.sc-val-d  { font-size: 1.3rem; font-weight: 800; color: #1f2937; line-height: 1.1; letter-spacing: -0.02em; margin-top: 0.2rem; }
.sc-sub    { font-size: 0.68rem; color: rgba(255,255,255,0.46); }
.sc-sub-d  { font-size: 0.68rem; color: #9ca3af; }

.prog-wrap { background:#f3f0ff; border-radius:99px; height:8px; overflow:hidden; margin:0.4rem 0 0.2rem; }
.prog-fill { height:8px; border-radius:99px; background:linear-gradient(90deg,#7c3aed,#f97316); }
.prog-row  { display:flex; justify-content:space-between; font-size:0.65rem; color:#9ca3af; font-weight:500; }

.chart-wrap {
  background: #faf5ff;
  border-radius: 18px;
  border: 1.5px solid #ede9fe;
  padding: 1rem 1rem 0.5rem;
  margin-bottom: 0.85rem;
}
.chart-hint { font-size:0.65rem; color:#c4b5fd; text-align:center; margin-top:0.2rem; }
.sec-lbl { font-size:0.62rem; font-weight:700; color:#9ca3af; text-transform:uppercase; letter-spacing:0.09em; margin:1rem 0 0.5rem; }

.stTextInput>div>div>input,
.stNumberInput>div>div>input {
  background:#f9fafb !important; border:1.5px solid #e5e7eb !important;
  border-radius:10px !important; color:#1f2937 !important;
  font-weight:500 !important; font-size:0.85rem !important;
}
.stTextInput>div>div>input:focus,
.stNumberInput>div>div>input:focus {
  border-color:#7c3aed !important; background:#faf5ff !important;
  box-shadow:0 0 0 3px rgba(124,58,237,0.12) !important;
}
label,.stTextInput label,.stNumberInput label,.stDateInput label,.stSelectbox label {
  color:#374151 !important; font-size:0.78rem !important; font-weight:600 !important;
}
.stButton>button {
  background:linear-gradient(135deg,#7c3aed,#a855f7) !important;
  color:#fff !important; border:none !important; border-radius:10px !important;
  padding:0.55rem 1.2rem !important; font-weight:700 !important;
  font-size:0.82rem !important; width:100% !important;
  box-shadow:0 3px 12px rgba(124,58,237,0.25) !important;
  transition:all 0.18s !important;
}
.stButton>button:hover { transform:translateY(-1px) !important; box-shadow:0 5px 16px rgba(124,58,237,0.35) !important; }
.stDateInput>div>div>input { background:#f9fafb !important; border:1.5px solid #e5e7eb !important; border-radius:10px !important; color:#1f2937 !important; }
.stSelectbox>div>div { background:#f9fafb !important; border:1.5px solid #e5e7eb !important; border-radius:10px !important; color:#1f2937 !important; }
.stSuccess{background:#f0fdf4 !important;border-color:#86efac !important;color:#16a34a !important;border-radius:10px !important;}
.stError{background:#fef2f2 !important;border-color:#fca5a5 !important;color:#dc2626 !important;border-radius:10px !important;}
.stInfo{background:#faf5ff !important;border-color:#c4b5fd !important;color:#7c3aed !important;border-radius:10px !important;}
.streamlit-expanderHeader { color:#374151 !important; font-size:0.8rem !important; font-weight:600 !important; }
details { background:#f9fafb !important; border-radius:12px !important; border:1.5px solid #f0edf8 !important; }
hr { border-color:#f0edf8 !important; }
.stDataFrame { border-radius:12px !important; overflow:hidden !important; border:1.5px solid #f0edf8 !important; }
</style>
""", unsafe_allow_html=True)

DATA_FILE = "weight_data.json"

def load_data():
    if os.path.exists(DATA_FILE):
        with open(DATA_FILE) as f:
            return json.load(f)
    return {"weights": [], "goal_weight": None, "height_cm": 170}

def save_data(d):
    with open(DATA_FILE, "w") as f:
        json.dump(d, f, indent=2)

def ensure_keys(d):
    d.setdefault("weights", [])
    d.setdefault("goal_weight", None)
    d.setdefault("height_cm", 170)
    return d

def calc_bmi(w, h):
    return round(w / (h / 100) ** 2, 1)

def bmi_cat(b):
    if b < 18.5:
        return "Underweight"
    if b < 25:
        return "Normal"
    if b < 30:
        return "Overweight"
    return "Obese"

def weeks_to_goal(cur, goal, rate=1.0):
    d = cur - goal
    return round(d / rate, 1) if d > 0 else None

def eta_date(w):
    return (datetime.today() + timedelta(weeks=w)).strftime("%b %d, %Y")

def build_chart(weights, goal_weight, y_tick_step=5):
    df = pd.DataFrame(weights).sort_values("date")
    df["date"] = pd.to_datetime(df["date"])
    fig = go.Figure()

    baseline_val = df["weight"].min() - 4
    y_min = baseline_val
    y_max = df["weight"].max() + 2

    fig.add_trace(go.Scatter(
        x=df["date"], y=[baseline_val] * len(df),
        mode="lines", line=dict(color="rgba(0,0,0,0)", width=0),
        showlegend=False, hoverinfo="skip",
    ))
    fig.add_trace(go.Scatter(
        x=df["date"], y=df["weight"],
        fill="tonexty",
        fillcolor="rgba(124,58,237,0.13)",
        line=dict(color="rgba(0,0,0,0)", width=0),
        showlegend=False, hoverinfo="skip",
    ))

    if goal_weight:
        last_date = df["date"].iloc[-1]
        last_w = df["weight"].iloc[-1]
        weeks = weeks_to_goal(last_w, goal_weight)
        if weeks and weeks > 0:
            n_pts = int(weeks) + 2
            proj_dates = pd.date_range(last_date, periods=n_pts, freq="7D")
            proj_vals = [max(last_w - i, goal_weight) for i in range(n_pts)]

            fig.add_trace(go.Scatter(
                x=proj_dates, y=[baseline_val] * len(proj_dates),
                mode="lines", line=dict(color="rgba(0,0,0,0)", width=0),
                showlegend=False, hoverinfo="skip",
            ))
            fig.add_trace(go.Scatter(
                x=proj_dates, y=proj_vals,
                fill="tonexty",
                fillcolor="rgba(16,185,129,0.10)",
                line=dict(color="#10b981", width=2.5, dash="dot"),
                name="Projection (−1 kg/wk)",
                hovertemplate="<b>%{y:.1f} kg</b> projected<br>%{x|%b %d, %Y}<extra></extra>",
            ))

    fig.add_trace(go.Scatter(
        x=df["date"], y=df["weight"],
        mode="lines+markers",
        line=dict(color="#7c3aed", width=3, shape="spline"),
        marker=dict(size=7, color="#fff", line=dict(color="#7c3aed", width=2.5)),
        name="Weight",
        hovertemplate="<b>%{y} kg</b>  •  %{x|%b %d, %Y}<extra></extra>",
    ))

    if goal_weight:
        fig.add_hline(
            y=goal_weight,
            line=dict(color="#f97316", width=1.5, dash="dash"),
            annotation_text=f"  🎯 Goal: {goal_weight} kg",
            annotation_position="bottom right",
            annotation_font=dict(color="#f97316", size=10, family="Plus Jakarta Sans"),
        )

    all_x = list(df["date"])
    if goal_weight:
        last_date = df["date"].iloc[-1]
        last_w = df["weight"].iloc[-1]
        weeks = weeks_to_goal(last_w, goal_weight)
        if weeks and weeks > 0:
            all_x.append(last_date + timedelta(weeks=int(weeks) + 1))

    fig.update_layout(
        paper_bgcolor="rgba(0,0,0,0)",
        plot_bgcolor="rgba(0,0,0,0)",
        margin=dict(l=0, r=4, t=8, b=0),
        height=340,
        xaxis=dict(
            showgrid=False, zeroline=False,
            tickfont=dict(color="#9ca3af", size=10, family="Plus Jakarta Sans"),
            tickformat="%b %d",
            showline=False,
            spikecolor="#c4b5fd", spikethickness=1, spikemode="across",
            range=[df["date"].min(), all_x[-1]] if all_x else None,
            dtick="M1",
        ),
        yaxis=dict(
            showgrid=True, gridcolor="rgba(124,58,237,0.07)",
            zeroline=False,
            tickfont=dict(color="#9ca3af", size=10, family="Plus Jakarta Sans"),
            ticksuffix=" kg", dtick=y_tick_step,
            range=[y_min, y_max],
        ),
        legend=dict(
            font=dict(color="#6b7280", size=10, family="Plus Jakarta Sans"),
            bgcolor="rgba(0,0,0,0)", orientation="h", y=-0.18,
        ),
        hovermode="x unified",
        hoverlabel=dict(
            bgcolor="#1f2937",
            font=dict(color="#fff", size=11, family="Plus Jakarta Sans"),
            bordercolor="#374151",
        ),
        dragmode="pan",
    )
    return fig

state = ensure_keys(load_data())

nav_left, nav_mid, nav_right = st.columns([2, 4, 2])
with nav_left:
    st.markdown('<div style="padding:0.55rem 0 0 1.5rem"><span class="nav-logo">⚖️ WeightTracker</span></div>', unsafe_allow_html=True)
with nav_mid:
    page = st.radio(
        "nav", ["📊 Dashboard", "➕ Log Weight", "⚙️ Settings"],
        horizontal=True, label_visibility="collapsed"
    )
with nav_right:
    st.markdown('<div style="text-align:right;font-size:0.72rem;color:#9ca3af;font-weight:600;padding:0.6rem 1.4rem 0 0">Personal tracker</div>', unsafe_allow_html=True)

st.markdown('<hr style="margin:0 0 0 0">', unsafe_allow_html=True)

if page == "📊 Dashboard":
    weights = state.get("weights", [])
    goal = state.get("goal_weight")
    height = state.get("height_cm")

    st.markdown('<div class="page-body">', unsafe_allow_html=True)

    if not weights:
        st.markdown('<div style="text-align:center;padding:4rem 1rem"><div style="font-size:3rem">📭</div><div style="font-size:1rem;font-weight:700;color:#1f2937;margin-top:0.5rem">No entries yet</div><div style="font-size:0.8rem;color:#9ca3af;margin-top:0.2rem">Go to Log Weight to start tracking</div></div>', unsafe_allow_html=True)
    else:
        df = pd.DataFrame(weights).sort_values("date")
        cw = df["weight"].iloc[-1]
        sw = df["weight"].iloc[0]
        total_l = round(sw - cw, 2)
        prev_w = df["weight"].iloc[-2] if len(df) > 1 else cw
        delta = round(cw - prev_w, 2)
        bmi_val = calc_bmi(cw, height) if height else None
        weeks = weeks_to_goal(cw, goal) if goal else None
        rem = max(round(cw - goal, 1), 0) if goal else None

        top_a, top_b, top_c = st.columns([1.35, 1, 1])

        with top_a:
            chip = f'<div class="chip-good">▼ {abs(delta):.2f} kg</div>' if delta < 0 else (f'<div class="chip-bad">▲ {abs(delta):.2f} kg</div>' if delta > 0 else '<div class="chip-flat">— no change</div>')
            st.markdown(f'''
            <div class="hero-card">
              <div class="hero-lbl">Current Weight</div>
              <div class="hero-weight">{cw}<span class="hero-unit">kg</span></div>
              {chip}
            </div>
            ''', unsafe_allow_html=True)

        with top_b:
            st.markdown(f'''
            <div class="stat-card sc-purple">
              <div>
                <div class="sc-lbl">Total Lost</div>
                <div class="sc-val">{abs(total_l):.1f} kg</div>
              </div>
              <div class="sc-sub">since start</div>
            </div>
            ''', unsafe_allow_html=True)

        with top_c:
            if goal:
                st.markdown(f'''
                <div class="stat-card sc-orange">
                  <div>
                    <div class="sc-lbl">Remaining</div>
                    <div class="sc-val">{rem} kg</div>
                  </div>
                  <div class="sc-sub">to {goal} kg</div>
                </div>
                ''', unsafe_allow_html=True)
            else:
                st.markdown(f'''
                <div class="stat-card sc-soft">
                  <div>
                    <div class="sc-lbl-d">Entries</div>
                    <div class="sc-val-d">{len(weights)}</div>
                  </div>
                  <div class="sc-sub-d">weigh-ins</div>
                </div>
                ''', unsafe_allow_html=True)

        bot_a, bot_b, bot_c = st.columns([1.35, 1, 1])

        with bot_a:
            if goal and sw > goal:
                pct = min(100, max(0, round((sw - cw) / (sw - goal) * 100, 1)))
                st.markdown(f'''
                <div style="padding-top:0.15rem">
                  <div style="display:flex;justify-content:space-between;margin-bottom:4px">
                    <span style="font-size:0.62rem;font-weight:700;color:#9ca3af;text-transform:uppercase;letter-spacing:0.08em">Progress to goal</span>
                    <span style="font-size:0.78rem;font-weight:800;background:linear-gradient(135deg,#7c3aed,#f97316);-webkit-background-clip:text;-webkit-text-fill-color:transparent">{pct}%</span>
                  </div>
                  <div class="prog-wrap"><div class="prog-fill" style="width:{pct}%"></div></div>
                  <div class="prog-row"><span>{sw} kg start</span><span>{goal} kg goal</span></div>
                </div>
                ''', unsafe_allow_html=True)

        with bot_b:
            if goal and weeks:
                st.markdown(f'''
                <div class="stat-card sc-dark">
                  <div>
                    <div class="sc-lbl">Goal Date</div>
                    <div class="sc-val" style="font-size:1rem">{eta_date(weeks)}</div>
                  </div>
                  <div class="sc-sub">{weeks} weeks left</div>
                </div>
                ''', unsafe_allow_html=True)
            elif goal:
                st.markdown(f'''
                <div class="stat-card sc-dark">
                  <div>
                    <div class="sc-lbl">Goal</div>
                    <div class="sc-val">🎉 Done!</div>
                  </div>
                  <div class="sc-sub">{goal} kg reached</div>
                </div>
                ''', unsafe_allow_html=True)

        with bot_c:
            if bmi_val:
                st.markdown(f'''
                <div class="stat-card sc-soft">
                  <div>
                    <div class="sc-lbl-d">BMI</div>
                    <div class="sc-val-d">{bmi_val}</div>
                  </div>
                  <div class="sc-sub-d">{bmi_cat(bmi_val)}</div>
                </div>
                ''', unsafe_allow_html=True)
            else:
                st.markdown(f'''
                <div class="stat-card sc-soft">
                  <div>
                    <div class="sc-lbl-d">Entries</div>
                    <div class="sc-val-d">{len(weights)}</div>
                  </div>
                  <div class="sc-sub-d">weigh-ins</div>
                </div>
                ''', unsafe_allow_html=True)

        st.markdown('<div class="sec-lbl">Weight Over Time</div>', unsafe_allow_html=True)
        tick_choice = st.selectbox(
            "Graph scale",
            options=[1, 2, 5, 10],
            index=2,
            format_func=lambda x: f"{x} kg groups",
            key="graph_tick_step"
        )
        st.markdown('<div class="chart-wrap">', unsafe_allow_html=True)
        fig = build_chart(weights, goal, y_tick_step=tick_choice)
        st.plotly_chart(fig, use_container_width=True, config={
            "displayModeBar": True,
            "scrollZoom": True,
            "modeBarButtonsToRemove": ["select2d","lasso2d","autoScale2d","resetScale2d","toImage"],
            "displaylogo": False
        })
        st.markdown('</div>', unsafe_allow_html=True)
        st.markdown('<div class="chart-hint">Scroll to zoom · drag to pan · hover for values</div>', unsafe_allow_html=True)

        with st.expander("📋 All entries"):
            df_show = df[["date", "weight"]].copy()
            df_show.columns = ["Date", "Weight (kg)"]
            df_show = df_show.sort_values("Date", ascending=False).reset_index(drop=True)
            st.dataframe(df_show, use_container_width=True, hide_index=True)

    st.markdown('</div>', unsafe_allow_html=True)

elif page == "➕ Log Weight":
    st.markdown('<div class="inner-page">', unsafe_allow_html=True)
    st.markdown('<div class="sec-lbl">New Entry</div>', unsafe_allow_html=True)
    with st.form("log_form", clear_on_submit=True):
        d = st.date_input("Date", value=datetime.today())
        w = st.number_input("Weight (kg)", min_value=20.0, max_value=300.0, value=70.0, step=0.1, format="%.1f")
        n = st.text_input("Note (optional)", placeholder="e.g. after gym, morning weigh-in")
        ok = st.form_submit_button("💾  Save Entry")
    if ok:
        ds = d.strftime("%Y-%m-%d")
        state["weights"] = [x for x in state["weights"] if x["date"] != ds]
        state["weights"].append({"date": ds, "weight": w, "note": n})
        save_data(state)
        st.success(f"✅ Saved {w} kg on {ds}!")
        st.rerun()

    if state["weights"]:
        st.markdown('<hr><div class="sec-lbl">Delete Entry</div>', unsafe_allow_html=True)
        df_del = pd.DataFrame(state["weights"]).sort_values("date", ascending=False)
        opts = [f"{r['date']} — {r['weight']} kg" for _, r in df_del.iterrows()]
        to_del = st.selectbox("Select entry", opts, label_visibility="collapsed")
        if st.button("🗑️  Delete selected"):
            del_date = to_del.split(" — ")[0]
            state["weights"] = [x for x in state["weights"] if x["date"] != del_date]
            save_data(state)
            st.success("Deleted.")
            st.rerun()
    st.markdown('</div>', unsafe_allow_html=True)

elif page == "⚙️ Settings":
    st.markdown('<div class="inner-page">', unsafe_allow_html=True)
    st.markdown('<div class="sec-lbl">Goals & Profile</div>', unsafe_allow_html=True)
    with st.form("set_form"):
        gw = st.number_input(
            "Goal weight (kg)",
            min_value=20.0,
            max_value=300.0,
            value=float(state["goal_weight"]) if state["goal_weight"] else 70.0,
            step=0.5,
            format="%.1f",
        )
        ht = st.number_input(
            "Height (cm)",
            min_value=100,
            max_value=250,
            value=int(state["height_cm"]) if state["height_cm"] else 165,
            step=1,
        )
        sv = st.form_submit_button("💾  Save Settings")
    if sv:
        state["goal_weight"] = gw
        state["height_cm"] = ht
        save_data(state)
        st.success("Settings saved!")
    st.markdown('</div>', unsafe_allow_html=True)