// Uses the HF Inference API for sentence-transformers/all-MiniLM-L6-v2
const HF_URL =
  "https://api-inference.huggingface.co/pipeline/feature-extraction/sentence-transformers/all-MiniLM-L6-v2";
const DIM = 384;

export default async function handler(request) {
  if (request.method !== "POST") {
    return json({ error: "Method not allowed" }, 405);
  }

  let text = "";
  try {
    const body = await request.json();
    text = (body.text || "").trim();
    if (!text) throw new Error("Empty text");
  } catch (e) {
    return json({ success: false, error: `parse: ${e.message}`, embedding: [] }, 400);
  }

  const token = process.env.HF_API_KEY || "";
  const headers = { "Content-Type": "application/json" };
  if (token) headers["Authorization"] = `Bearer ${token}`;

  let resp;
  try {
    resp = await fetch(HF_URL, {
      method: "POST",
      headers,
      body: JSON.stringify({ inputs: text, options: { wait_for_model: true } }),
    });
  } catch (fetchErr) {
    return json({
      success: false,
      error: `fetch_failed: ${fetchErr?.message || String(fetchErr)}`,
      embedding: [],
    }, 500);
  }

  if (!resp.ok) {
    const msg = await resp.text().catch(() => "unknown");
    return json({ success: false, error: `hf_api_${resp.status}: ${msg}`, embedding: [] }, 500);
  }

  try {
    const result = await resp.json();
    // HF pipeline returns [[float x 384]] for single input
    const embedding = Array.isArray(result[0]) ? result[0] : result;
    return json({ success: true, dimension: DIM, embedding });
  } catch (e) {
    return json({ success: false, error: `parse_response: ${e.message}`, embedding: [] }, 500);
  }
}

function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

export const config = { runtime: "edge" };
