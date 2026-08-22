import { useState, useEffect } from "react";
import { useParams, Link, useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { knowledgeGraphApi, documentsApi } from "../services/apiClient";
import { motion, AnimatePresence } from "framer-motion";
import { X, Loader2, Target, BrainCircuit, ZoomIn, ZoomOut, Maximize } from "lucide-react";
import type { ConceptDto } from "../types";
import { PageWrapper } from "../components/layout/PageWrapper";
import { forceSimulation, forceLink, forceManyBody, forceCenter, forceCollide, forceX, forceY } from "d3-force";
import { TransformWrapper, TransformComponent } from "react-zoom-pan-pinch";

export function KnowledgeGraph() {
  const { documentId } = useParams<{ documentId: string }>();
  const navigate = useNavigate();
  const [selectedConcept, setSelectedConcept] = useState<ConceptDto | null>(null);
  
  // States for d3 simulation
  const [nodes, setNodes] = useState<any[]>([]);
  const [edges, setEdges] = useState<any[]>([]);

  const { data: documents } = useQuery({
    queryKey: ["documents"],
    queryFn: documentsApi.getAll,
  });
  const readyDocuments = documents?.filter(d => d.status === 'READY') || [];

  const { data, isLoading, error } = useQuery({
    queryKey: ["knowledgeGraph", documentId || "global"],
    queryFn: () => documentId ? knowledgeGraphApi.getForDocument(documentId) : knowledgeGraphApi.getGlobal(),
  });

  // Initialize D3 Force Simulation
  useEffect(() => {
    if (!data?.concepts || data.concepts.length === 0) {
      setNodes([]);
      setEdges([]);
      return;
    }

    const d3Nodes = data.concepts.map((c) => ({
      ...c,
      radius: 20 + (c.importanceScore * 15), // Size based on importance
    }));

    const d3Edges = (data.relationships || []).map((rel) => ({
      ...rel,
      source: rel.sourceConceptId,
      target: rel.targetConceptId,
    }));

    const simulation = forceSimulation(d3Nodes as any)
      .force("link", forceLink(d3Edges).id((d: any) => d.id).distance(180))
      .force("charge", forceManyBody().strength(-1000).distanceMax(500))
      .force("center", forceCenter(0, 0))
      .force("collide", forceCollide().radius((d: any) => d.radius + 20).iterations(3))
      .force("x", forceX().strength(0.05))
      .force("y", forceY().strength(0.05))
      .on("tick", () => {
        setNodes([...d3Nodes]);
        setEdges([...d3Edges]);
      });

    return () => {
      simulation.stop();
    };
  }, [data]);

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <Loader2 className="w-8 h-8 text-indigo-500 animate-spin" />
      </div>
    );
  }

  if (error || !data || data.concepts.length === 0) {
    return (
      <div className="flex flex-col min-h-screen items-center justify-center text-zinc-500">
        <BrainCircuit className="w-12 h-12 mb-4 text-zinc-300" />
        <p className="text-lg font-medium text-zinc-700 dark:text-zinc-300">No Knowledge Graph available.</p>
        <p className="mt-2 text-sm max-w-md text-center">
          {documentId 
            ? "We haven't extracted any concepts from this document yet. Try uploading a more text-heavy document or check back after processing."
            : "You don't have any extracted concepts yet. Upload some documents and let our AI analyze them!"}
        </p>
        <Link to="/documents" className="mt-6 px-6 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors font-medium shadow-sm">
          Go to Documents
        </Link>
      </div>
    );
  }

  return (
    <PageWrapper className="flex h-[calc(100vh-8rem)] gap-6 relative">
      <div className="flex-1 glass-card rounded-2xl overflow-hidden relative min-h-0 flex flex-col">
        <div className="p-4 border-b border-slate-200/50 dark:border-zinc-800 flex flex-col gap-4 z-20 bg-white/50 dark:bg-zinc-950/50 backdrop-blur-md">
          <div className="flex items-center justify-between">
            <h2 className="text-xl font-bold bg-gradient-to-r from-indigo-600 to-purple-600 bg-clip-text text-transparent dark:from-indigo-400 dark:to-purple-400">
              {documentId ? "Document Knowledge Graph" : "Global Knowledge Graph"}
            </h2>
          </div>
          
          <div className="flex items-center gap-2 overflow-x-auto custom-scrollbar pb-1">
            <button
              onClick={() => navigate('/graph')}
              className={`px-5 py-2 rounded-full text-sm font-semibold whitespace-nowrap transition-all shadow-sm ${
                !documentId 
                  ? 'bg-indigo-600 text-white ring-2 ring-indigo-600 ring-offset-2 dark:ring-offset-zinc-950' 
                  : 'bg-white dark:bg-zinc-900 text-zinc-700 dark:text-zinc-300 hover:bg-zinc-50 border border-zinc-200 dark:border-zinc-800'
              }`}
            >
              Global View
            </button>
            {readyDocuments.map(doc => (
              <button
                key={doc.id}
                onClick={() => navigate(`/graph/${doc.id}`)}
                className={`px-5 py-2 rounded-full text-sm font-semibold whitespace-nowrap transition-all shadow-sm ${
                  documentId === doc.id
                    ? 'bg-indigo-600 text-white ring-2 ring-indigo-600 ring-offset-2 dark:ring-offset-zinc-950' 
                    : 'bg-white dark:bg-zinc-900 text-zinc-700 dark:text-zinc-300 hover:bg-zinc-50 border border-zinc-200 dark:border-zinc-800'
                }`}
              >
                {doc.originalFileName}
              </button>
            ))}
          </div>
        </div>
        
        <div className="flex-1 relative bg-slate-50/30 dark:bg-zinc-950/30 overflow-hidden">
          <TransformWrapper
            initialScale={1}
            minScale={0.1}
            maxScale={4}
            centerOnInit={true}
            wheel={{ step: 0.1 }}
            disablePadding={true}
          >
            {({ zoomIn, zoomOut, resetTransform }) => (
              <div className="w-full h-full">
                <div className="absolute bottom-6 right-6 z-20 flex flex-col gap-2 glass-card p-2 rounded-xl shadow-lg">
                  <button onClick={() => zoomIn()} className="p-2 hover:bg-zinc-100 dark:hover:bg-zinc-800 rounded-lg text-zinc-600 dark:text-zinc-300 transition-colors" title="Zoom In">
                    <ZoomIn className="w-5 h-5" />
                  </button>
                  <button onClick={() => zoomOut()} className="p-2 hover:bg-zinc-100 dark:hover:bg-zinc-800 rounded-lg text-zinc-600 dark:text-zinc-300 transition-colors" title="Zoom Out">
                    <ZoomOut className="w-5 h-5" />
                  </button>
                  <div className="w-full h-px bg-zinc-200 dark:bg-zinc-800 my-1"></div>
                  <button onClick={() => resetTransform()} className="p-2 hover:bg-zinc-100 dark:hover:bg-zinc-800 rounded-lg text-zinc-600 dark:text-zinc-300 transition-colors" title="Fit to Screen">
                    <Maximize className="w-5 h-5" />
                  </button>
                </div>

                <TransformComponent wrapperStyle={{ width: "100%", height: "100%" }} contentStyle={{ width: "100%", height: "100%" }}>
                  <svg viewBox="-1500 -1500 3000 3000" preserveAspectRatio="xMidYMid meet" className="cursor-grab active:cursor-grabbing" style={{ width: "100%", height: "100%" }}>
                    <defs>
                      <marker id="arrowhead" markerWidth="10" markerHeight="7" refX="28" refY="3.5" orient="auto">
                        <polygon points="0 0, 10 3.5, 0 7" fill="#94a3b8" className="dark:fill-zinc-600" />
                      </marker>
                      <marker id="arrowhead-active" markerWidth="10" markerHeight="7" refX="28" refY="3.5" orient="auto">
                        <polygon points="0 0, 10 3.5, 0 7" fill="#6366f1" />
                      </marker>
                      <filter id="glow" x="-20%" y="-20%" width="140%" height="140%">
                        <feGaussianBlur stdDeviation="5" result="blur" />
                        <feComposite in="SourceGraphic" in2="blur" operator="over" />
                      </filter>
                    </defs>
                    
                    {/* Draw Edges */}
                    <g className="edges">
                      {edges.map((edge) => {
                        // D3 replaces source/target strings with node objects after forceLink resolves
                        if (typeof edge.source === 'string' || typeof edge.target === 'string') return null;
                        // x can legitimately be 0 — use null check, not falsy check
                        if (edge.source.x == null || edge.target.x == null) return null;
                        const isSelected = selectedConcept && (edge.source.id === selectedConcept.id || edge.target.id === selectedConcept.id);
                        
                        // Calculate curved path (quadratic bezier)
                        const dx = edge.target.x - edge.source.x;
                        const dy = edge.target.y - edge.source.y;
                        const dr = Math.sqrt(dx * dx + dy * dy) * 1.5; // Curve factor
                        const path = `M${edge.source.x},${edge.source.y}A${dr},${dr} 0 0,1 ${edge.target.x},${edge.target.y}`;

                        return (
                          <path
                            key={edge.id}
                            d={path}
                            fill="none"
                            stroke={isSelected ? "#6366f1" : "#cbd5e1"}
                            strokeWidth={isSelected ? 2.5 : 1.5}
                            className={isSelected ? "stroke-indigo-500" : "dark:stroke-zinc-800 transition-colors duration-300"}
                            markerEnd={isSelected ? "url(#arrowhead-active)" : "url(#arrowhead)"}
                          />
                        );
                      })}
                    </g>
                    
                    {/* Draw Nodes */}
                    <g className="nodes">
                      {nodes.map((node) => {
                        if (node.x == null) return null;
                        const isSelected = selectedConcept?.id === node.id;
                        
                        // Check if node is a neighbor of selected
                        const isNeighbor = selectedConcept && edges.some(e =>
                          typeof e.source !== 'string' && typeof e.target !== 'string' && (
                            (e.source.id === selectedConcept.id && e.target.id === node.id) ||
                            (e.target.id === selectedConcept.id && e.source.id === node.id)
                          )
                        );
                        
                        const isFaded = selectedConcept && !isSelected && !isNeighbor;

                        return (
                          <g 
                            key={node.id} 
                            transform={`translate(${node.x}, ${node.y})`} 
                            onClick={() => setSelectedConcept(node)}
                            className={`cursor-pointer outline-none transition-opacity duration-300 ${isFaded ? 'opacity-30' : 'opacity-100'}`}
                          >
                            <circle
                              r={isSelected ? node.radius + 4 : node.radius}
                              fill={isSelected ? "#4f46e5" : "#ffffff"}
                              stroke={isSelected ? "#818cf8" : "#e0e7ff"}
                              strokeWidth={isSelected ? 4 : 3}
                              filter={isSelected ? "url(#glow)" : ""}
                              className="dark:fill-zinc-900 dark:stroke-indigo-900/60 transition-all duration-300 shadow-2xl"
                            />
                            {/* Inner core for nodes to look premium */}
                            {!isSelected && (
                              <circle
                                r={node.radius - 8}
                                fill="#f5f3ff"
                                className="dark:fill-zinc-800"
                              />
                            )}
                            <text
                              y={node.radius + 16}
                              textAnchor="middle"
                              className={`font-semibold pointer-events-none ${
                                isSelected 
                                  ? "fill-indigo-700 dark:fill-indigo-300 text-sm font-bold" 
                                  : "fill-slate-600 dark:fill-zinc-400 text-xs"
                              }`}
                              style={{ textShadow: "0 1px 3px rgba(255,255,255,0.8)" }}
                            >
                              {node.name.length > 25 ? node.name.substring(0, 22) + "..." : node.name}
                            </text>
                          </g>
                        );
                      })}
                    </g>
                  </svg>
                </TransformComponent>
              </div>
            )}
          </TransformWrapper>
        </div>
      </div>

      <AnimatePresence>
        {selectedConcept && (
          <motion.div 
            initial={{ opacity: 0, x: 20, scale: 0.95 }}
            animate={{ opacity: 1, x: 0, scale: 1 }}
            exit={{ opacity: 0, x: 20, scale: 0.95 }}
            transition={{ duration: 0.2, ease: "easeOut" }}
            className="absolute top-20 right-6 max-h-[calc(100%-6rem)] glass-card p-0 rounded-2xl shadow-2xl w-80 z-30 flex flex-col shrink-0 border border-slate-200/50 dark:border-zinc-800 overflow-hidden"
          >
            <div className="p-5 border-b border-slate-100 dark:border-zinc-800 flex items-start justify-between bg-gradient-to-br from-indigo-50/80 to-purple-50/80 dark:from-indigo-900/20 dark:to-purple-900/20">
              <div>
                <h3 className="font-bold text-lg text-indigo-950 dark:text-indigo-100 pr-4 leading-tight mb-1">{selectedConcept.name}</h3>
                <span className="inline-block px-2 py-0.5 rounded-full bg-indigo-100 text-indigo-700 dark:bg-indigo-900 dark:text-indigo-300 text-[10px] font-bold uppercase tracking-wider">
                  Concept
                </span>
              </div>
              <button onClick={() => setSelectedConcept(null)} className="p-1.5 bg-white/50 dark:bg-zinc-900/50 hover:bg-white dark:hover:bg-zinc-800 rounded-full text-zinc-500 shadow-sm transition-all">
                <X className="w-4 h-4" />
              </button>
            </div>
            
            <div className="p-5 flex-1 overflow-y-auto space-y-6 custom-scrollbar bg-white/50 dark:bg-zinc-950/50">
              <div>
                <h4 className="text-[11px] uppercase tracking-wider font-bold text-slate-400 mb-2">Description</h4>
                <p className="text-sm text-slate-700 dark:text-zinc-300 leading-relaxed bg-slate-50 dark:bg-zinc-900/50 p-3 rounded-xl border border-slate-100 dark:border-zinc-800">{selectedConcept.description}</p>
              </div>

              <div className="space-y-4">
                <h4 className="text-[11px] uppercase tracking-wider font-bold text-slate-400 mb-2">Metrics</h4>
                
                <div className="glass-card p-3 rounded-xl">
                  <div className="flex justify-between text-xs mb-1.5">
                    <span className="font-semibold text-slate-600 dark:text-zinc-400">Importance</span>
                    <span className="font-bold text-indigo-600">{(selectedConcept.importanceScore * 100).toFixed(0)}%</span>
                  </div>
                  <div className="h-2 bg-slate-100 dark:bg-zinc-800 rounded-full overflow-hidden shadow-inner">
                    <div className="h-full bg-gradient-to-r from-indigo-400 to-indigo-600 rounded-full" style={{ width: `${selectedConcept.importanceScore * 100}%` }}></div>
                  </div>
                </div>

                <div className="glass-card p-3 rounded-xl">
                  <div className="flex justify-between text-xs mb-1.5">
                    <span className="font-semibold text-slate-600 dark:text-zinc-400">Difficulty</span>
                    <span className="font-bold text-rose-600">{(selectedConcept.difficultyScore * 100).toFixed(0)}%</span>
                  </div>
                  <div className="h-2 bg-slate-100 dark:bg-zinc-800 rounded-full overflow-hidden shadow-inner">
                    <div className="h-full bg-gradient-to-r from-rose-400 to-rose-600 rounded-full" style={{ width: `${selectedConcept.difficultyScore * 100}%` }}></div>
                  </div>
                </div>
              </div>

              <div>
                <h4 className="text-[11px] uppercase tracking-wider font-bold text-slate-400 mb-3">Connections</h4>
                <div className="space-y-2">
                  {data.relationships
                    .filter(r => r.sourceConceptId === selectedConcept.id || r.targetConceptId === selectedConcept.id)
                    .map(rel => {
                      const isSource = rel.sourceConceptId === selectedConcept.id;
                      const connectedName = isSource ? rel.targetConceptName : rel.sourceConceptName;
                      return (
                        <div key={rel.id} className="text-xs flex flex-col bg-white dark:bg-zinc-900 p-3 rounded-xl border border-slate-100 dark:border-zinc-800 shadow-sm hover:border-indigo-200 transition-colors">
                          <span className="font-bold text-slate-900 dark:text-zinc-100 mb-1">{connectedName}</span>
                          <div className="flex items-center gap-1.5 text-slate-500">
                            <span className={`px-1.5 py-0.5 rounded text-[10px] font-semibold ${isSource ? 'bg-emerald-50 text-emerald-600' : 'bg-amber-50 text-amber-600'}`}>
                              {isSource ? 'Target' : 'Source'}
                            </span>
                            <span className="opacity-50">•</span>
                            <span>{rel.relationshipType}</span>
                          </div>
                        </div>
                      );
                  })}
                </div>
              </div>
            </div>
            
            <div className="p-4 border-t border-slate-100 dark:border-zinc-800 bg-slate-50 dark:bg-zinc-900">
              <Link 
                to={`/chat/${selectedConcept.documentId || documentId || ''}`}
                className="w-full flex items-center justify-center gap-2 bg-indigo-600 hover:bg-indigo-700 text-white shadow-md shadow-indigo-500/20 text-sm font-bold py-2.5 rounded-xl transition-all"
              >
                <Target className="w-4 h-4" /> Discuss Concept
              </Link>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </PageWrapper>
  );
}
