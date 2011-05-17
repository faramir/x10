#ifndef X10AUX_DESERIALIZATION_DISPATCHER_H
#define X10AUX_DESERIALIZATION_DISPATCHER_H

#include <x10aux/config.h>

#include <x10aux/ref.h>

namespace x10 { namespace lang { class Ref; } }

namespace x10aux {

    class deserialization_buffer;

    typedef ref<x10::lang::Ref> (*Deserializer)(deserialization_buffer &buf);
    template<> inline const char *typeName<Deserializer>() { return "Deserializer"; }

    typedef void *(*BufferFinder)(deserialization_buffer &buf, x10_int len);
    template<> inline const char *typeName<BufferFinder>() { return "BufferFinder"; }

    typedef void (*Notifier)(deserialization_buffer &buf, x10_int len);
    template<> inline const char *typeName<Notifier>() { return "Notifier"; }

    typedef x10_short serialization_id_t;

    template<> inline const char *typeName<serialization_id_t>() { return "serialization_id_t"; }

    class DeserializationDispatcher {
        protected:
        static DeserializationDispatcher *it;

        
        BufferFinder *put_bfinder_v;
        Notifier *put_notifier_v;
        size_t put_sz;

        BufferFinder *get_bfinder_v;
        Notifier *get_notifier_v;
        size_t get_sz;

        Deserializer *deser_v;
        size_t deser_sz;

        size_t next_id;

        public:
        DeserializationDispatcher () : put_bfinder_v(NULL), put_notifier_v(NULL), put_sz(0),
                                       get_bfinder_v(NULL), get_notifier_v(NULL), get_sz(0),
                                       deser_v(NULL), deser_sz(0), next_id(1) { }
        ~DeserializationDispatcher () {
            ::free(put_bfinder_v); // do not use GC
            ::free(get_bfinder_v); // do not use GC
            ::free(deser_v); // do not use GC
        }

        
        template<class T> static ref<T> create(deserialization_buffer &buf);
        template<class T> static ref<T> create(deserialization_buffer &buf,
                                               serialization_id_t id);

        ref<x10::lang::Ref> create_(deserialization_buffer &buf);
        ref<x10::lang::Ref> create_(deserialization_buffer &buf, serialization_id_t id);

        static serialization_id_t addDeserializer(Deserializer deser, bool is_async=false);
        serialization_id_t addDeserializer_(Deserializer deser, bool is_async);

        static serialization_id_t addPutFunctions(BufferFinder bfinder, Notifier notifier);
        serialization_id_t addPutFunctions_(BufferFinder bfinder, Notifier notifier);
        static BufferFinder getPutBufferFinder(serialization_id_t id);
        BufferFinder getPutBufferFinder_(serialization_id_t id);
        static Notifier getPutNotifier(serialization_id_t id);
        Notifier getPutNotifier_(serialization_id_t id);

        static serialization_id_t addGetFunctions(BufferFinder bfinder, Notifier notifier);
        serialization_id_t addGetFunctions_(BufferFinder bfinder, Notifier notifier);
        static BufferFinder getGetBufferFinder(serialization_id_t id);
        BufferFinder getGetBufferFinder_(serialization_id_t id);
        static Notifier getGetNotifier(serialization_id_t id);
        Notifier getGetNotifier_(serialization_id_t id);

        static void registerHandlers();
        void registerHandlers_();
    };

    inline BufferFinder DeserializationDispatcher::getPutBufferFinder (serialization_id_t id) {
        return it->getPutBufferFinder_(id); 
    }

    inline BufferFinder DeserializationDispatcher::getGetBufferFinder (serialization_id_t id) {
        return it->getGetBufferFinder_(id); 
    }

    inline Notifier DeserializationDispatcher::getPutNotifier (serialization_id_t id) {
        return it->getPutNotifier_(id); 
    }

    inline Notifier DeserializationDispatcher::getGetNotifier (serialization_id_t id) {
        return it->getGetNotifier_(id); 
    }

    template<class T> ref<T> DeserializationDispatcher::create(deserialization_buffer &buf,
                                                               serialization_id_t id) {
        return static_cast<ref<T> >(it->create_(buf,id)); 
    }

    template<class T> ref<T> DeserializationDispatcher::create(deserialization_buffer &buf) {
        return static_cast<ref<T> >(it->create_(buf)); 
    }
    
    template<> inline const char *typeName<DeserializationDispatcher>()
    { return "DeserializationDispatcher"; }
}

#endif
// vim:tabstop=4:shiftwidth=4:expandtab