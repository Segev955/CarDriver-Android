package classes

class ObdEntry {
    // ---- PRIVATE PROPERTIES ---- //
    private var user_id: String? = null
    private var obd_id: String? = null
    private var key: String? = null

    constructor()

    /**
     * constructor
     */
    constructor(user_id: String?, obd_id: String?, key: String?) {
        this.user_id = user_id
        this.obd_id = obd_id
        this.key = key
    }

    // ---- GETTERS ---- //
    fun getUserId(): String? {
        return user_id
    }

    fun getObdId(): String? {
        return obd_id
    }

    fun getKey(): String? {
        return key
    }
}
