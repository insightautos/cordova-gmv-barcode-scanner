export function getKeyByValue<TObject>(
  obj: { [s: string]: TObject },
  value: TObject,
): string {
  const keys = Object.keys(obj);
  const index = keys.map((key) => obj[key]).indexOf(value);
  return keys[index] || String(value);
}
