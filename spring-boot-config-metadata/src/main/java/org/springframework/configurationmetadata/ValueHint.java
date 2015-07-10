package org.springframework.configurationmetadata;

/**
 * Hint for a value a given property may have. Provide the value and
 * an optional description.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
public class ValueHint {

    private Object value;
    private String description;

    /**
     * Return the hint value.
     */
    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    /**
     * Return the description of the value, if any.
     */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "ValueHint{" + "value=" + this.value + ", description='"
                + this.description + '\'' + '}';
    }
}
