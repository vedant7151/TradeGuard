export default function Toast({ message, onClose }) {
  if (!message) return null;

  return (
    <div className="toast" data-testid="error-toast" role="alert">
      <span>{message}</span>
      <button onClick={onClose} aria-label="dismiss">
        &times;
      </button>
    </div>
  );
}
