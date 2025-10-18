package healthcare.model;

/**
 * Abstract base class for all people in the healthcare system.
 * Demonstrates inheritance and encapsulation principles.
 * This class provides common properties and methods for Staff and Patient.
 */
public abstract class Person {
    protected String id;
    protected String name;
    protected String email;
    protected String phone;

    public Person(String id, String name, String email, String phone) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    @Override
    public String toString() {
        return "Person{id='" + id + "', name='" + name + "', email='" + email + "', phone='" + phone + "'}";
    }
}
