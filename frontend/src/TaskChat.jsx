import { CircleHelp, LoaderCircle, MessageSquareText, Plus, RotateCcw, Send, Sparkles } from 'lucide-react';

const salonExample = 'Find me a salon in Lekki for knotless braids tomorrow before 3 PM. My budget is ₦25,000, and don’t suggest anything that clashes with my calendar.';

export default function TaskChat({ messages, prompt, setPrompt, phase, error, onSubmit, onNewConversation }) {
  const busy = phase === 'processing';
  const submit = event => {
    event.preventDefault();
    if (!busy) onSubmit(prompt);
  };
  const onKeyDown = event => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      if (!busy) onSubmit(prompt);
    }
  };

  return <div className="panel chat-panel">
    <div className="chat-header">
      <div><div className="section-label"><Sparkles size={14}/> LIFEADMIN WORKSPACE</div><h3>Task conversation</h3><p>Sample results only · no external services connected</p></div>
      <button type="button" className="outline-button" onClick={onNewConversation}><RotateCcw size={13}/> New conversation</button>
    </div>
    <div className="chat-stream" role="log" aria-live="polite" aria-label="Task conversation">
      {!messages.length ? <div className="chat-empty">
        <span><MessageSquareText size={21}/></span><b>What would you like handled?</b>
        <p>Describe the task and I’ll show how the sample workflow interprets it.</p>
        <div className="chat-suggestions"><button type="button" onClick={()=>onSubmit(salonExample)}>Find a salon for knotless braids <Send size={12}/></button><button type="button" onClick={()=>onSubmit('Find a reliable cleaner in Lekki for this weekend')}>Find a home cleaner in Lekki <Send size={12}/></button></div>
      </div> : messages.map((message,index)=><div className={'chat-message '+message.role} key={index}>
        <span className="chat-avatar">{message.role==='user'?'AO':<Sparkles size={14}/>}</span><div className="chat-bubble"><small>{message.role==='user'?'YOU':'LIFEADMIN · SIMULATED'}</small><p>{message.text}</p></div>
      </div>)}
      {busy&&<div className="chat-message assistant"><span className="chat-avatar"><Sparkles size={14}/></span><div className="chat-bubble processing-bubble"><small>LIFEADMIN · SIMULATED</small><div className="chat-thinking"><LoaderCircle className="spin" size={14}/> Checking the request and sample provider data…</div><div className="skeleton-line short"/><div className="skeleton-line"/></div></div>}
    </div>
    <form className="chat-composer" onSubmit={submit}>
      <textarea className="chat-input" aria-label="Write a message to LifeAdmin" value={prompt} disabled={busy} onChange={event=>setPrompt(event.target.value)} onKeyDown={onKeyDown} placeholder="Message LifeAdmin…" rows={2}/>
      <div className="composer-foot"><span>Enter to send · Shift + Enter for a new line</span><button className="send-button" type="submit" disabled={busy}>{busy?<><LoaderCircle className="spin" size={15}/> Searching…</>:<>Send message <Plus size={14}/></>}</button></div>
      {error&&<div className="inline-error" role="alert"><CircleHelp size={15}/><span>{error}{prompt.toLowerCase().includes('[demo search error]')&&<small className="error-hint">Test error trigger: remove [demo search error] from the message, then retry.</small>}</span>{phase==='error'&&<button type="button" className="retry-button" onClick={()=>onSubmit(prompt)}>Retry</button>}</div>}
    </form>
  </div>;
}
