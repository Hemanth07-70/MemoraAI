import { createBrowserRouter } from "react-router-dom";
import { Layout } from "../components/layout/Layout";

// Lazy load pages for better performance (we'll create these files next)
import { Dashboard } from "../pages/Dashboard";
import { Documents } from "../pages/Documents";
import { Chat } from "../pages/Chat";
import { KnowledgeGraph } from "../pages/KnowledgeGraph";
import { Memory } from "../pages/Memory";
import { Revision } from "../pages/Revision";
import { QuizCenter } from "../pages/QuizCenter";
import { Profile } from "../pages/Profile";
import { Settings } from "../pages/Settings";

export const router = createBrowserRouter([
  {
    path: "/",
    element: <Layout />,
    children: [
      { index: true, element: <Dashboard /> },
      { path: "documents", element: <Documents /> },
      { path: "chat", element: <Chat /> },
      { path: "chat/:documentId", element: <Chat /> },
      { path: "graph", element: <KnowledgeGraph /> },
      { path: "graph/:documentId", element: <KnowledgeGraph /> },
      { path: "memory", element: <Memory /> },
      { path: "revision", element: <Revision /> },
      { path: "quiz", element: <QuizCenter /> },
      { path: "quiz/:documentId", element: <QuizCenter /> },
      { path: "profile", element: <Profile /> },
      { path: "settings", element: <Settings /> },
    ],
  },
]);
