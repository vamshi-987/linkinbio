export default function LinkList({ links, onDelete, onMoveUp, onMoveDown }) {
  return (
    <ul className="space-y-2">
      {links.map((link, i) => (
        <li key={link.id} className="flex items-center justify-between border rounded px-3 py-2">
          <div>
            <p className="font-medium">{link.title}</p>
            <p className="text-sm text-gray-500">{link.url}</p>
          </div>
          <div className="flex gap-1">
            <button onClick={() => onMoveUp(i)} disabled={i === 0} className="px-2">↑</button>
            <button onClick={() => onMoveDown(i)} disabled={i === links.length - 1} className="px-2">↓</button>
            <button onClick={() => onDelete(link.id)} className="px-2 text-red-500">✕</button>
          </div>
        </li>
      ))}
    </ul>
  );
}