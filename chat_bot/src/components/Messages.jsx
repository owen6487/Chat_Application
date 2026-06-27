import { useState, useEffect, useRef } from 'react'
import axios from "axios";
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { IoSend } from "react-icons/io5";




function Messages() {

    const Backend_url = import.meta.env.VITE_API_URL;

    const getid=(key)=>{
        const storage =key ==="userId"?localStorage:sessionStorage;
        let id=storage.getItem(key);
        if(!id){
            id =crypto.randomUUID();
            storage.setItem(key,id);
        }
        return id;
    }

   
    const [chats, setChats] = useState([]);
    const[loading, setLoading] = useState(false);
    const[error, setError] = useState(false);
    const [userId] = useState(getid('userId'));
    const [chatId] = useState(getid('chatId'));
    const[sessionId]=useState(getid('sessionId'));
    const[input, setInput] = useState("");
    const[connected, setConnected]=useState(false);


    

   
    const stompClientRef=useRef(null);
    const bottomRef = useRef(null);
    const textarea = useRef(null);


    const chathistory = async ()=>{
        try {
            const res = await axios.get(Backend_url+"/api/chat/history",{
                params:{chatId},
            });
            setChats(Array.isArray(res.data) ? res.data : []);
            setLoading(true);
        } catch (error) {
            console.log(error);
            setLoading(false);
            setError(true);
        }
    }

   

    const handlesend = async()=>{
        const text = input.trim();
        if(!text || !stompClientRef.current.connected) return;

        const usermessage = {
            chatId,
            userId,
            sessionId,
            sender:"user",
            message:text,
            timestamp:new Date().toISOString(),
            
        }
        setChats((prev)=>[...prev,usermessage]);
        setLoading(true);
        setError(false);

        stompClientRef.current.publish({
            destination:"/app/send",
            body:JSON.stringify(usermessage),
        });
        setInput("");
        // reset textarea height after clearing input
        if(textarea.current) textarea.current.style.height = 'auto';
    }

    useEffect(() => {
        chathistory();

        const client = new Client ({
            webSocketFactory:()=>new SockJS(Backend_url +"/ws"),
            reconnectDelay:5000,
            onConnect:()=>{
                setConnected(true);
                console.log("connected");
                client.subscribe(`/topic/chat/${chatId}`,message =>{
                    const data = JSON.parse(message.body);
                    setChats((prev)=>[...prev,data]);
                    setLoading(false);
                })
            },
            onStompError: error =>{
                console.error('STOMP error', error);
                setError(true);
                setLoading(false);
            },
            onDisconnect:()=>{
                setConnected(false);
                console.log("disconnected");
            }
        });
        client.activate();
        stompClientRef.current=client;

        return () => {
            client.deactivate();
        };
    }, [chatId]);

    
    const handleinputChange=(e)=>{
        setInput(e.target.value);
        if(textarea.current){
            textarea.current.style.height='auto';
            textarea.current.style.height=textarea.current.scrollHeight + 'px';
        }
    };
    
    useEffect(()=>{
        bottomRef.current?.scrollIntoView({behaviour:"smooth"})
    },[chats])
    return (
        <div className='flex flex-col  bg-black h-screen w-screen text-white'>
                <div className='flex flex-col  p-6 gap-4 bg-black border-b  border-gray-800'>
                    <h1 className='text-blue-400 font-bold text-3xl  leading-relaxed text-center  '>chat with zora</h1>
                    <p className='text-gray-400 font-semibold text-[15px] leading-relaxed max-w-3xl mx-auto'>chat with zora,a AI assistant designed to help you with information and guidance related to getting into university in kenya and getting information about different courses.</p>
                </div>
                   <div className='flex-1 overflow-y-auto px-4 py-4 space-y-6 '>
                     {chats.map((msg,idx)=>{
                        const isUser=msg.sender==="user" || msg.role==="user"
                        return(
                            <div className={`flex ${isUser ? 'justify-end' : 'justify-start'} gap-2 `}>
                                {!isUser && <img src="https://i.pinimg.com/736x/d0/bd/71/d0bd718decbaa8f95bab16f78482d624.jpg" alt="Zuri" className="h-8 w-8 rounded-full object-cover flex-shrink-0 self-end" />}
                                <div className={`flex flex-col max-w-[75%] ${isUser ? 'items-end' : 'items-start'}`}>
                                  <div className={`px-4 py-2 rounded-2xl text-[15px] leading-relaxed break-words 
                                     ${isUser ? 'bg-blue-500' : 'bg-green-800'}`}>
                                          {isUser
                                            ? <span style={{ whiteSpace: 'pre-wrap' }}>{msg.message}</span>
                                            : <div className="markdown-body"><ReactMarkdown remarkPlugins={[remarkGfm]}>{msg.message}</ReactMarkdown></div>
                                          }
                                  </div>
                                  <span className="text-xs text-gray-500 mt-1 px-1">
                                      {msg.timestamp ?
                                       new Date(msg.timestamp).toLocaleTimeString([], {hour:"2-digit" ,minute:"2-digit"}):
                                      ""
                                      }
                                  </span>
                                 </div>
                                
                                
                            </div>
                        );
                     })}

                     {loading && (
                        <div className='flex justify-start items-end gap-2'>
                            <div>
                                <img className='h-10 w-10 rounded-full object-cover' src="https://i.pinimg.com/736x/d0/bd/71/d0bd718decbaa8f95bab16f78482d624.jpg" alt="Zuri" />
                            </div>
                            <div className='animate-pulse bg-gray-800 px-4 py-2 rounded-2xl text-[15px] leading-relaxed'>
                                Zora typing...
                            </div>
                        </div>
                     )}

                     <div ref={bottomRef} />

                    </div>
                    {error &&   <div className="flex items-center justify-between gap-3 mx-4 mb-2 px-4 py-3 bg-red-900/40 border border-red-700 rounded-xl text-red-200 text-sm">
                                  <span>Something went wrong. Please try again.</span>
                                  <button onClick={() => setError(false)} className="text-red-300 hover:text-white text-lg leading-none">×</button>
                             </div>}

                     {/*textarea — pinned to bottom outside the scroll area*/}
                     <div className='flex items-center gap-3 px-4 py-3 border-t border-gray-800 bg-black'>
                        <textarea
                            ref={textarea}
                            value={input}
                            onChange={handleinputChange}
                            className="flex-1 p-2 rounded-2xl bg-gray-900 text-white border border-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none overflow-hidden"
                            onKeyDown={(e)=>{ if(e.key==="Enter" && !e.shiftKey){ e.preventDefault(); handlesend(); } }}
                            rows="1"
                            placeholder='Type your message'
                            required
                        />
                        <button onClick={handlesend} className="p-3 rounded-full bg-blue-500 hover:bg-blue-600 active:bg-blue-700 transition-colors duration-200 flex items-center justify-center"><IoSend size={25} color='white' /></button>
                      </div>
            
        </div>

        
    )
}

export default Messages;