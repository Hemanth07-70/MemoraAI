const HF_URL =
  "https://api-inference.huggingface.co/models/sentence-transformers/all-MiniLM-L6-v2";
const DIM = 384;

export default async function handler(request) {
  if (request.method !== "POST") {
    return json({ error: "Method not allowed" }, 405);
  }

  try {
    const body = await request.json();
    const text = (body.text || "").trim();
    if (!text) throw new Error("Empty text");

    const token = process.env.HF_API_KEY || "";
    const headers = { "Content-Type": "application/json" };
    if (token) headers["Authorization"] = `Bearer ${token}`;

    const resp = await fetch(HF_URL, {
      method: "POST",
      headers,
      body: JSON.stringify({ inputs: text, options: { wait_for_model: true } }),
    });

    if (!resp.ok) {
      const msg = await resp.text();
      throw new Error(`HF API ${resp.status}: ${msg}`);
    }

    const result = await resp.json();
    const embedding = Array.isArray(result[0]) ? result[0] : result;

    return json({ success: true, dimension: DIM, embedding });
  } catch (e) {
    return json({ success: false, error: e.message, embedding: [] }, 500);
  }
}

function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

export const config = { runtime: "edge" };
